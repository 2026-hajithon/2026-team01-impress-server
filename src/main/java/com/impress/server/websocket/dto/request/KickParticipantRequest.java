package com.impress.server.websocket.dto.request;

public record KickParticipantRequest(
        Long targetParticipantId
) {
}