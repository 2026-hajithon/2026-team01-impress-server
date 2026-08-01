package com.impress.server.websocket.dto;

public record WebSocketResponse<T>(
        WebSocketEventType type,
        T data
) {
}
