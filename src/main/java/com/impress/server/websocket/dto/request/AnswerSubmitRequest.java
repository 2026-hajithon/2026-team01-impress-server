package com.impress.server.websocket.dto.request;

public record AnswerSubmitRequest(
        Long roundId,
        String textAnswer,
        Long selectedOptionId,
        Long pickedParticipantId
) {
}
