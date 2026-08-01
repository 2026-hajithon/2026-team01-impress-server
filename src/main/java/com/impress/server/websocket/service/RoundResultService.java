package com.impress.server.websocket.service;

import com.impress.server.answer.domain.Answer;
import com.impress.server.answer.repository.AnswerRepository;
import com.impress.server.game.domain.GameRound;
import com.impress.server.participant.domain.ConnectionStatus;
import com.impress.server.participant.domain.Participant;
import com.impress.server.participant.repository.ParticipantRepository;
import com.impress.server.question.domain.QuestionOption;
import com.impress.server.room.domain.Room;
import com.impress.server.websocket.dto.response.RoundResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.impress.server.game.domain.GameRoundStatus;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoundResultService {

    private final ParticipantRepository participantRepository;
    private final AnswerRepository answerRepository;

    @Transactional
    public Optional<RoundResultResponse> createIfReady(
            GameRound gameRound,
            Room room
    ) {
        List<Participant> roomParticipants =
                participantRepository
                        .findAllByRoomOrderByIdAsc(room);

        List<Answer> answers =
                answerRepository
                        .findAllByGameRoundOrderByIdAsc(
                                gameRound
                        );

        if (!hasAllRequiredAnswers(
                gameRound,
                roomParticipants,
                answers
        )) {
            return Optional.empty();
        }

        gameRound.openResult();

        return Optional.of(
                createRoundResult(
                        gameRound,
                        roomParticipants,
                        answers
                )
        );
    }

    @Transactional
    public Optional<RoundResultResponse> createOnTimeout(
            GameRound gameRound,
            Room room
    ) {
        if (gameRound.getStatus()
                != GameRoundStatus.ANSWERING) {
            return Optional.empty();
        }

        LocalDateTime deadlineAt =
                gameRound.getDeadlineAt();

        if (deadlineAt == null
                || LocalDateTime.now().isBefore(deadlineAt)) {
            return Optional.empty();
        }

        List<Participant> roomParticipants =
                participantRepository
                        .findAllByRoomOrderByIdAsc(room);

        List<Answer> answers =
                answerRepository
                        .findAllByGameRoundOrderByIdAsc(
                                gameRound
                        );

        gameRound.openResult();

        return Optional.of(
                createRoundResult(
                        gameRound,
                        roomParticipants,
                        answers
                )
        );
    }

    private boolean hasAllRequiredAnswers(
            GameRound gameRound,
            List<Participant> roomParticipants,
            List<Answer> answers
    ) {
        Set<Long> requiredParticipantIds =
                roomParticipants.stream()
                        .filter(participant ->
                                participant.getConnectionStatus()
                                        == ConnectionStatus.CONNECTED
                        )
                        .map(Participant::getId)
                        .collect(Collectors.toSet());

        if (gameRound.getQuestion().getQuestionType()
                == com.impress.server.question.domain.QuestionType.BLANK
                && gameRound.getTargetParticipant() != null) {
            requiredParticipantIds.remove(
                    gameRound.getTargetParticipant().getId()
            );
        }

        Set<Long> submittedParticipantIds =
                answers.stream()
                        .map(answer ->
                                answer.getRespondentParticipant()
                                        .getId()
                        )
                        .collect(Collectors.toSet());

        return submittedParticipantIds.containsAll(
                requiredParticipantIds
        );
    }

    private RoundResultResponse createRoundResult(
            GameRound gameRound,
            List<Participant> roomParticipants,
            List<Answer> answers
    ) {
        RoundResultResponse.Result result =
                switch (gameRound.getQuestion()
                        .getQuestionType()) {
                    case BLANK ->
                            createBlankResult(answers);

                    case INDIVIDUAL_CHOICE ->
                            createIndividualChoiceResult(
                                    gameRound,
                                    answers
                            );

                    case COMMON_VOTE ->
                            createCommonVoteResult(
                                    roomParticipants,
                                    answers
                            );
                };

        return new RoundResultResponse(
                gameRound.getId(),
                gameRound.getRoundOrder(),
                gameRound.getQuestion().getQuestionType(),
                result
        );
    }

    private RoundResultResponse.BlankResult
    createBlankResult(
            List<Answer> answers
    ) {
        List<RoundResultResponse.BlankAnswer>
                blankAnswers =
                answers.stream()
                        .map(answer ->
                                new RoundResultResponse
                                        .BlankAnswer(
                                        answer
                                                .getRespondentParticipant()
                                                .getId(),
                                        answer
                                                .getRespondentParticipant()
                                                .getName(),
                                        answer.getTextContent()
                                )
                        )
                        .toList();

        return new RoundResultResponse.BlankResult(
                blankAnswers
        );
    }

    private RoundResultResponse.IndividualChoiceResult
    createIndividualChoiceResult(
            GameRound gameRound,
            List<Answer> answers
    ) {
        Map<Long, Long> optionCounts =
                answers.stream()
                        .filter(answer ->
                                answer.getSelectedOption() != null
                        )
                        .collect(
                                Collectors.groupingBy(
                                        answer ->
                                                answer
                                                        .getSelectedOption()
                                                        .getId(),
                                        Collectors.counting()
                                )
                        );

        List<RoundResultResponse.OptionResult>
                optionResults =
                gameRound.getQuestion()
                        .getOptions()
                        .stream()
                        .sorted(
                                Comparator.comparingInt(
                                        QuestionOption::getDisplayOrder
                                )
                        )
                        .map(option ->
                                new RoundResultResponse.OptionResult(
                                        option.getId(),
                                        option.getContent(),
                                        option.getDisplayOrder(),
                                        optionCounts.getOrDefault(
                                                option.getId(),
                                                0L
                                        )
                                )
                        )
                        .toList();

        long maximumCount =
                optionResults.stream()
                        .mapToLong(
                                RoundResultResponse
                                        .OptionResult::count
                        )
                        .max()
                        .orElse(0L);

        List<Long> mostSelectedOptionIds =
                maximumCount == 0L
                        ? List.of()
                        : optionResults.stream()
                          .filter(option ->
                                  option.count()
                                  == maximumCount
                          )
                          .map(
                                  RoundResultResponse
                                  .OptionResult::optionId
                          )
                          .toList();

        Long targetParticipantId =
                gameRound.getTargetParticipant() == null
                        ? null
                        : gameRound.getTargetParticipant().getId();

        Long targetAnswerOptionId =
                answers.stream()
                        .filter(answer ->
                                targetParticipantId != null
                                        && answer
                                        .getRespondentParticipant()
                                        .getId()
                                        .equals(targetParticipantId)
                        )
                        .filter(answer ->
                                answer.getSelectedOption() != null
                        )
                        .map(answer ->
                                answer.getSelectedOption().getId()
                        )
                        .findFirst()
                        .orElse(null);

        return new RoundResultResponse
                .IndividualChoiceResult(
                targetAnswerOptionId,
                mostSelectedOptionIds,
                optionResults
        );
    }

    private RoundResultResponse.CommonVoteResult
    createCommonVoteResult(
            List<Participant> roomParticipants,
            List<Answer> answers
    ) {
        Map<Long, Long> participantVoteCounts =
                answers.stream()
                        .filter(answer ->
                                answer.getSelectedParticipant()
                                        != null
                        )
                        .collect(
                                Collectors.groupingBy(
                                        answer ->
                                                answer
                                                        .getSelectedParticipant()
                                                        .getId(),
                                        Collectors.counting()
                                )
                        );

        Map<Long, Participant> participantsById =
                roomParticipants.stream()
                        .collect(
                                Collectors.toMap(
                                        Participant::getId,
                                        Function.identity()
                                )
                        );

        List<RoundResultResponse.ParticipantVote> votes =
                participantVoteCounts.entrySet()
                        .stream()
                        .map(entry -> {
                            Participant participant =
                                    participantsById.get(
                                            entry.getKey()
                                    );

                            return new RoundResultResponse
                                    .ParticipantVote(
                                    participant.getId(),
                                    participant.getName(),
                                    entry.getValue()
                            );
                        })
                        .sorted(
                                Comparator
                                        .comparingLong(
                                                (RoundResultResponse
                                                         .ParticipantVote vote) ->
                                                        vote.count()
                                        )
                                        .reversed()
                                        .thenComparing(
                                                RoundResultResponse
                                                        .ParticipantVote
                                                        ::participantId
                                        )
                        )
                        .toList();

        return new RoundResultResponse.CommonVoteResult(
                votes
        );
    }
}
