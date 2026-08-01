package com.impress.server.websocket.exception;

import com.impress.server.websocket.dto.WebSocketEventType;
import com.impress.server.websocket.dto.response.WebSocketErrorResponse;
import com.impress.server.websocket.dto.response.WebSocketResponse;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
public class WebSocketExceptionAdvice {

    @MessageExceptionHandler(IllegalArgumentException.class)
    @SendToUser(destinations = "/queue/errors", broadcast = false)
    public WebSocketResponse<WebSocketErrorResponse> handleIllegalArgument(
            IllegalArgumentException exception
    ) {
        return createErrorResponse(
                "INVALID_REQUEST",
                exception.getMessage()
        );
    }

    @MessageExceptionHandler(IllegalStateException.class)
    @SendToUser(destinations = "/queue/errors", broadcast = false)
    public WebSocketResponse<WebSocketErrorResponse> handleIllegalState(
            IllegalStateException exception
    ) {
        return createErrorResponse(
                "INVALID_STATE",
                exception.getMessage()
        );
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser(destinations = "/queue/errors", broadcast = false)
    public WebSocketResponse<WebSocketErrorResponse> handleUnexpectedException(
            Exception exception
    ) {
        return createErrorResponse(
                "INTERNAL_ERROR",
                "서버 내부 오류가 발생했습니다."
        );
    }

    private WebSocketResponse<WebSocketErrorResponse> createErrorResponse(
            String code,
            String message
    ) {
        return new WebSocketResponse<>(
                WebSocketEventType.ERROR,
                new WebSocketErrorResponse(code, message)
        );
    }
}