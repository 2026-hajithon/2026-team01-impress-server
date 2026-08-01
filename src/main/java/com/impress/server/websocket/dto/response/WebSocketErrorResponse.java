package com.impress.server.websocket.dto.response;

public record WebSocketErrorResponse(
        String code,
        String message
) {
}