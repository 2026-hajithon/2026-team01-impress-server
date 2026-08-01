package com.impress.server.websocket.listener;

import com.impress.server.websocket.auth.StompPrincipal;
import com.impress.server.websocket.service.RoomWebSocketService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Component
public class WebSocketDisconnectEventListener {

    private final RoomWebSocketService roomWebSocketService;

    public WebSocketDisconnectEventListener(
            RoomWebSocketService roomWebSocketService
    ) {
        this.roomWebSocketService = roomWebSocketService;
    }

    @EventListener
    public void handleSessionDisconnect(
            SessionDisconnectEvent event
    ) {
        Principal principal = event.getUser();

        if (principal instanceof StompPrincipal stompPrincipal) {
            roomWebSocketService.disconnect(
                    stompPrincipal.participantId()
            );
        }
    }
}