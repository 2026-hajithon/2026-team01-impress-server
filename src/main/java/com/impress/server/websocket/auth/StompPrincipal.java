package com.impress.server.websocket.auth;

import java.security.Principal;

public record StompPrincipal(
        Long participantId
) implements Principal {

    @Override
    public String getName() {
        return participantId.toString();
    }
}