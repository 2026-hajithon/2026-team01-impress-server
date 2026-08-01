package com.impress.server.websocket.dto.response;

public record QuestionOptionResponse(
        Long optionId,
        String content,
        int displayOrder
) {
}