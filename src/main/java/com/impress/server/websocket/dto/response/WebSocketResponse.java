package com.impress.server.websocket.dto.response;

import com.impress.server.websocket.dto.WebSocketEventType;

public record WebSocketResponse<T>(
        WebSocketEventType type,
        T data
) {
}
