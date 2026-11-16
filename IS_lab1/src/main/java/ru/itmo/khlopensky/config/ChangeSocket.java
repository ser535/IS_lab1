package ru.itmo.khlopensky.config;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChangeSocket extends TextWebSocketHandler {
    public record Changed() {}

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), new ConcurrentWebSocketSessionDecorator(session, 2000, 65536));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
    }

    @TransactionalEventListener
    public void changed(Changed event) {
        for (WebSocketSession session : sessions.values()) {
            try {
                if (session.isOpen()) session.sendMessage(new TextMessage("changed"));
            } catch (IOException | RuntimeException exception) {
                sessions.remove(session.getId());
                try {
                    session.close(CloseStatus.SERVER_ERROR);
                } catch (IOException ignored) {
                }
            }
        }
    }
}
