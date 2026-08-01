package com.impress.server.websocket.dto.response;

import com.impress.server.question.domain.QuestionType;

import java.util.List;

public record RoundResultResponse(
        Long roundId,
        int roundOrder,
        QuestionType qType,
        Result result
) {

    public sealed interface Result
            permits BlankResult,
            IndividualChoiceResult,
            CommonVoteResult {
    }

    public record BlankResult(
            List<BlankAnswer> answers
    ) implements Result {
    }

    public record BlankAnswer(
            Long submitterId,
            String submitterName,
            String textAnswer
    ) {
    }

    public record IndividualChoiceResult(
            Long targetAnswerOptionId,
            List<Long> mostSelectedOptionIds,
            List<OptionResult> optionResults
    ) implements Result {
    }

    public record OptionResult(
            Long optionId,
            String content,
            int displayOrder,
            long count
    ) {
    }

    public record CommonVoteResult(
            List<ParticipantVote> votes
    ) implements Result {
    }

    public record ParticipantVote(
            Long participantId,
            String participantName,
            long count
    ) {
    }
}