package com.impress.server.websocket.controller;

import com.impress.server.websocket.auth.StompPrincipal;
import com.impress.server.websocket.service.RoomWebSocketService;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class RoomWebSocketController {

    private final RoomWebSocketService roomWebSocketService;

    public RoomWebSocketController(
            RoomWebSocketService roomWebSocketService
    ) {
        this.roomWebSocketService = roomWebSocketService;
    }

    @MessageMapping("/rooms/{roomCode}/enter")
    public void enter(
            @DestinationVariable String roomCode,
            Principal principal
    ) {
        if (!(principal instanceof StompPrincipal stompPrincipal)) {
            throw new MessageDeliveryException(
                    "참가자 인증 정보가 없습니다."
            );
        }

        roomWebSocketService.enter(
                roomCode,
                stompPrincipal.participantId()
        );
    }
}