package com.impress.server.websocket.dto.response;

import com.impress.server.question.domain.QuestionType;

import java.util.List;

public record RoundStartResponse(
        Long roundId,
        int roundOrder,
        int totalRounds,
        QuestionType qType,
        Long targetId,
        String question,
        List<QuestionOptionResponse> options,
        int timeLimit
) {
}