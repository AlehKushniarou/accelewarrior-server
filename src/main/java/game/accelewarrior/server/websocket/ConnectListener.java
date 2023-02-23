package game.accelewarrior.server.websocket;

import org.springframework.web.socket.WebSocketSession;

public interface ConnectListener {
    void handle(WebSocketSession session);
}
