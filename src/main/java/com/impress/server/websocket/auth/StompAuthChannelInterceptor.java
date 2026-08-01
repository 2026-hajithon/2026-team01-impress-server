package com.impress.server.websocket.auth;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String PARTICIPANT_ID_HEADER = "Participant-Id";

    @Override
    public Message<?> preSend(
            Message<?> message,
            MessageChannel channel
    ) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(
                        message,
                        StompHeaderAccessor.class
                );

        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String participantIdHeader =
                    accessor.getFirstNativeHeader(PARTICIPANT_ID_HEADER);

            if (participantIdHeader == null || participantIdHeader.isBlank()) {
                throw new MessageDeliveryException(
                        "Participant-Id 헤더가 필요합니다."
                );
            }

            try {
                Long participantId = Long.valueOf(participantIdHeader);

                if (participantId <= 0) {
                    throw new NumberFormatException();
                }

                accessor.setUser(new StompPrincipal(participantId));

            } catch (NumberFormatException e) {
                throw new MessageDeliveryException(
                        "Participant-Id는 양의 정수여야 합니다."
                );
            }
        }

        return message;
    }
}