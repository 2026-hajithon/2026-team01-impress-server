package com.impress.server.websocket.publisher;

import com.impress.server.websocket.dto.WebSocketEventType;
import com.impress.server.websocket.dto.response.WebSocketResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class WebSocketEventPublisher {

    private static final String ROOM_TOPIC_PREFIX = "/topic/rooms/";
    private static final String ERROR_QUEUE = "/queue/errors";

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketEventPublisher(
            SimpMessagingTemplate messagingTemplate
    ) {
        this.messagingTemplate = messagingTemplate;
    }

    // 방 참가자 전체에게 전송
    public <T> void broadcastToRoom(
            String roomCode,
            WebSocketEventType eventType,
            T data
    ) {
        String destination = ROOM_TOPIC_PREFIX + roomCode;

        WebSocketResponse<T> response =
                new WebSocketResponse<>(eventType, data);

        messagingTemplate.convertAndSend(destination, response);
    }

    // 요청을 잘못 보낸 한 명에게만 전송
    public <T> void sendErrorToParticipant(
            Long participantId,
            T errorData
    ) {
        WebSocketResponse<T> response =
                new WebSocketResponse<>(
                        WebSocketEventType.ERROR,
                        errorData
                );

        messagingTemplate.convertAndSendToUser(
                participantId.toString(),
                ERROR_QUEUE,
                response
        );
    }
}