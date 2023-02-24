package game.accelewarrior.server.websocket;

import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.standard.StandardWebSocketSession;

public interface ConnectListener {
    void handle(StandardWebSocketSession session);
}
