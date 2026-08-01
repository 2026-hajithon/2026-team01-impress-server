package com.impress.server.websocket.dto.response;

public record ParticipantSummaryResponse(
        Long participantId,
        String name,
        String role,
        String connectionStatus
) {
}