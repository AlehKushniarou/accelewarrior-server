package game.accelewarrior.server;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.ObjectMap;
import game.accelewarrior.server.actors.Warrior;
import game.accelewarrior.server.websocket.WebSocketHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.adapter.standard.StandardWebSocketSession;

import java.io.IOException;
import java.util.concurrent.ForkJoinPool;

@Component
public class GameLoop extends ApplicationAdapter {
    private static final float FRAME_RATE = 1 / 2f;
    private final WebSocketHandler socketHandler;
    private float lastRender = 0.0f;
    private final Json json;
    private final ObjectMap<String, Warrior> warriors = new ObjectMap<>();
    private final ForkJoinPool pool = ForkJoinPool.commonPool();

    public GameLoop(WebSocketHandler socketHandler, Json json) {
        this.socketHandler = socketHandler;
        this.json = json;
    }

    @Override
    public void create() {
        socketHandler.setConnectListener(session -> {
            Warrior warrior = new Warrior();
            warrior.setId(session.getId());
            warriors.put(session.getId(), warrior);
            try {
                session.getNativeSession().getBasicRemote().sendText(session.getId());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        socketHandler.setDisconnectListener(session -> {
            warriors.remove(session.getId());
        });
        socketHandler.setMessageListener(((session, message) -> {
            pool.execute(() -> {
                String type = message.getString("type");
                switch (type) {
                    case "state":
                        Warrior warrior = warriors.get(session.getId());
                        warrior.setAccelerometerX(message.getFloat("accelerometerX"));
                        warrior.setAccelerometerY(message.getFloat("accelerometerY"));
                        warrior.setJustTouched(message.getBoolean("justTouched"));
                        break;
                    default: throw new RuntimeException("Unknown WS object type: " + type);
                }
            });
            System.out.println(session.getId() + " said " + message);
        }));
    }

    @Override
    public void render() {
        lastRender += Gdx.graphics.getDeltaTime();
        if (lastRender >= FRAME_RATE) {
            for (ObjectMap.Entry<String, Warrior> warriorEntry : warriors) {
                Warrior warrior = warriorEntry.value;
                warrior.act(lastRender);
            }

            lastRender = 0;

            pool.execute(() ->{
                String stateJson = json.toJson(warriors);
                for (StandardWebSocketSession session : socketHandler.getSessions()) {
                    try {
                        session.getNativeSession().getBasicRemote().sendText(stateJson);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }
}
