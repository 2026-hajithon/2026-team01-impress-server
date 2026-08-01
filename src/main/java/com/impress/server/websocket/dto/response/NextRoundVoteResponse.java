package com.impress.server.websocket.dto.response;

public record NextRoundVoteResponse(
        Long roundId,
        long votedCount,
        long requiredCount
) {
}