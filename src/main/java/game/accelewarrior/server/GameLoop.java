package game.accelewarrior.server;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
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
    private final ForkJoinPool pool = ForkJoinPool.commonPool();

    private final ObjectMap<String, Warrior> warriors = new ObjectMap<>();
    private final Array<Warrior> stateToSend = new Array<>();

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
                session
                        .getNativeSession()
                        .getBasicRemote()
                        .sendText(
                                String.format("{\"class\":\"sessionKey\",\"id\":\"%s\"}", session.getId())
                        );
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        socketHandler.setDisconnectListener(session -> {
            sendToAll(
                    String.format("{\"class\":\"evict\",\"id\":\"%s\"}", session.getId())
            );
            warriors.remove(session.getId());
        });
        socketHandler.setMessageListener(((session, message) -> {
            pool.execute(() -> {
                String type = message.get("type").asText();
                switch (type) {
                    case "state":
                        Warrior warrior = warriors.get(session.getId());
                        warrior.setAccelerometerX((float) message.get("accelerometerX").asDouble());
                        warrior.setAccelerometerY((float) message.get("accelerometerY").asDouble());
                        warrior.setJustTouched(message.get("justTouched").asBoolean());
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
            stateToSend.clear();
            for (ObjectMap.Entry<String, Warrior> warriorEntry : warriors) {
                Warrior warrior = warriorEntry.value;
                warrior.act(lastRender);
                stateToSend.add(warrior);
            }

            lastRender = 0;
            String stateJson = json.toJson(stateToSend);

            sendToAll(stateJson);
        }
    }

    private void sendToAll(String json) {
        pool.execute(() ->{
            for (StandardWebSocketSession session : socketHandler.getSessions()) {
                try {
                    if (session.isOpen()) {
                        session.getNativeSession().getBasicRemote().sendText(json);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
