package com.impress.server.websocket.controller;

import com.impress.server.websocket.auth.StompPrincipal;
import com.impress.server.websocket.dto.WebSocketEventType;
import com.impress.server.websocket.dto.request.KickParticipantRequest;
import com.impress.server.websocket.dto.response.RoundStartResponse;
import com.impress.server.websocket.publisher.WebSocketEventPublisher;
import com.impress.server.websocket.service.GameWebSocketService;
import com.impress.server.websocket.service.RoomWebSocketService;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class RoomWebSocketController {

    private final RoomWebSocketService roomWebSocketService;
    private final GameWebSocketService gameWebSocketService;
    private final WebSocketEventPublisher eventPublisher;

    public RoomWebSocketController(
            RoomWebSocketService roomWebSocketService,
            GameWebSocketService gameWebSocketService,
            WebSocketEventPublisher eventPublisher
    ) {
        this.roomWebSocketService = roomWebSocketService;
        this.gameWebSocketService = gameWebSocketService;
        this.eventPublisher = eventPublisher;
    }

    @MessageMapping("/rooms/{roomCode}/enter")
    public void enter(
            @DestinationVariable String roomCode,
            Principal principal
    ) {
        StompPrincipal stompPrincipal =
                requireStompPrincipal(principal);

        roomWebSocketService.enter(
                roomCode,
                stompPrincipal.participantId()
        );
    }

    @MessageMapping("/rooms/{roomCode}/kick")
    public void kick(
            @DestinationVariable String roomCode,
            KickParticipantRequest request,
            Principal principal
    ) {
        StompPrincipal stompPrincipal =
                requireStompPrincipal(principal);

        if (request == null
                || request.targetParticipantId() == null
                || request.targetParticipantId() <= 0) {
            throw new MessageDeliveryException(
                    "올바른 강퇴 대상 ID가 필요합니다."
            );
        }

        roomWebSocketService.kick(
                roomCode,
                stompPrincipal.participantId(),
                request.targetParticipantId()
        );
    }

    @MessageMapping("/rooms/{roomCode}/start")
    public void start(
            @DestinationVariable String roomCode,
            Principal principal
    ) {
        StompPrincipal stompPrincipal =
                requireStompPrincipal(principal);

        RoundStartResponse response =
                gameWebSocketService.start(
                        roomCode,
                        stompPrincipal.participantId()
                );

        eventPublisher.broadcastToRoom(
                roomCode,
                WebSocketEventType.ROUND_START,
                response
        );
    }

    private StompPrincipal requireStompPrincipal(
            Principal principal
    ) {
        if (principal instanceof StompPrincipal stompPrincipal) {
            return stompPrincipal;
        }

        throw new MessageDeliveryException(
                "참가자 인증 정보가 없습니다."
        );
    }
}