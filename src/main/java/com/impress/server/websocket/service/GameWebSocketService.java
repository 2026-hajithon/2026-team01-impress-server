package com.impress.server.websocket.service;

import com.impress.server.game.domain.GameRound;
import com.impress.server.game.domain.GameSession;
import com.impress.server.game.domain.GameSessionStatus;
import com.impress.server.game.repository.GameRoundRepository;
import com.impress.server.game.repository.GameSessionRepository;
import com.impress.server.participant.domain.ConnectionStatus;
import com.impress.server.participant.domain.Participant;
import com.impress.server.participant.domain.ParticipantRole;
import com.impress.server.participant.repository.ParticipantRepository;
import com.impress.server.question.domain.Question;
import com.impress.server.question.domain.QuestionType;
import com.impress.server.question.repository.QuestionRepository;
import com.impress.server.room.domain.Room;
import com.impress.server.room.repository.RoomRepository;
import com.impress.server.websocket.dto.response.QuestionOptionResponse;
import com.impress.server.websocket.dto.response.RoundStartResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.impress.server.game.domain.GameRoundStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GameWebSocketService {

    private static final int MIN_PARTICIPANTS = 2;
    private static final int MAX_PARTICIPANTS = 8;
    private static final int COMMON_ROUND_COUNT = 1;
    private static final int BLANK_TIME_LIMIT_SECONDS = 60;
    private static final int INDIVIDUAL_CHOICE_TIME_LIMIT_SECONDS = 15;
    private static final int COMMON_VOTE_TIME_LIMIT_SECONDS = 15;

    private final RoomRepository roomRepository;
    private final ParticipantRepository participantRepository;
    private final QuestionRepository questionRepository;
    private final GameSessionRepository gameSessionRepository;
    private final GameRoundRepository gameRoundRepository;

    @Transactional
    public RoundStartResponse start(
            String roomCode,
            Long requesterParticipantId
    ) {
        Room room = roomRepository.findByCodeForUpdate(roomCode)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 방입니다."
                        )
                );

        Participant requester =
                participantRepository.findByIdAndRoom(
                                requesterParticipantId,
                                room
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "해당 방의 참가자가 아닙니다."
                                )
                        );

        validateHost(requester);

        if (gameSessionRepository.existsByRoomAndStatus(
                room,
                GameSessionStatus.PLAYING
        )) {
            throw new IllegalStateException(
                    "이미 진행 중인 게임이 있습니다."
            );
        }

        List<Participant> participants =
                findConnectedParticipants(room);

        validateParticipantCount(participants);

        List<Question> personalQuestions =
                findShuffledPersonalQuestions(
                        participants.size()
                );

        List<Question> commonQuestions =
                findShuffledCommonQuestions();

        Collections.shuffle(participants);

        int totalRounds =
                participants.size() + COMMON_ROUND_COUNT;

        GameSession gameSession =
                gameSessionRepository.save(
                        new GameSession(
                                room,
                                totalRounds
                        )
                );

        List<GameRound> rounds = createRounds(
                gameSession,
                participants,
                personalQuestions,
                commonQuestions
        );

        GameRound firstRound = rounds.get(0);

        int timeLimitSeconds =
                getTimeLimitSeconds(
                        firstRound.getQuestion().getQuestionType()
                );

        firstRound.start(
                LocalDateTime.now()
                        .plusSeconds(timeLimitSeconds)
        );

        room.startGame();

        gameRoundRepository.saveAllAndFlush(rounds);

        return createRoundStartResponse(
                firstRound,
                timeLimitSeconds
        );
    }

    @Transactional
    public RoundStartResponse startNextRound(
            GameRound gameRound
    ) {
        if (gameRound.getStatus()
                != GameRoundStatus.PENDING) {
            throw new IllegalStateException(
                    "시작할 수 없는 라운드입니다."
            );
        }

        int timeLimitSeconds =
                getTimeLimitSeconds(
                        gameRound.getQuestion()
                                .getQuestionType()
                );

        gameRound.start(
                LocalDateTime.now()
                        .plusSeconds(timeLimitSeconds)
        );

        gameRoundRepository.saveAndFlush(gameRound);

        return createRoundStartResponse(
                gameRound,
                timeLimitSeconds
        );
    }

    private void validateHost(Participant requester) {
        if (requester.getRole() != ParticipantRole.HOST) {
            throw new IllegalStateException(
                    "방장만 게임을 시작할 수 있습니다."
            );
        }
    }

    private List<Participant> findConnectedParticipants(
            Room room
    ) {
        return new ArrayList<>(
                participantRepository
                        .findAllByRoomOrderByIdAsc(room)
                        .stream()
                        .filter(participant ->
                                participant.getConnectionStatus()
                                        == ConnectionStatus.CONNECTED
                        )
                        .toList()
        );
    }

    private void validateParticipantCount(
            List<Participant> participants
    ) {
        if (participants.size() < MIN_PARTICIPANTS) {
            throw new IllegalStateException(
                    "게임 시작에는 최소 2명이 필요합니다."
            );
        }

        if (participants.size() > MAX_PARTICIPANTS) {
            throw new IllegalStateException(
                    "게임에는 최대 8명까지 참여할 수 있습니다."
            );
        }
    }

    private List<Question> findShuffledPersonalQuestions(
            int requiredCount
    ) {
        List<Question> questions = new ArrayList<>();

        questions.addAll(
                questionRepository
                        .findAllByQuestionTypeOrderByIdAsc(
                                QuestionType.BLANK
                        )
        );

        questions.addAll(
                questionRepository
                        .findAllByQuestionTypeOrderByIdAsc(
                                QuestionType.INDIVIDUAL_CHOICE
                        )
        );

        if (questions.size() < requiredCount) {
            throw new IllegalStateException(
                    "개인 질문이 부족합니다."
            );
        }

        Collections.shuffle(questions);

        return new ArrayList<>(
                questions.subList(0, requiredCount)
        );
    }

    private List<Question> findShuffledCommonQuestions() {
        List<Question> questions = new ArrayList<>(
                questionRepository
                        .findAllByQuestionTypeOrderByIdAsc(
                                QuestionType.COMMON_VOTE
                        )
        );

        if (questions.size() < COMMON_ROUND_COUNT) {
            throw new IllegalStateException(
                    "공통 투표 질문이 부족합니다."
            );
        }

        Collections.shuffle(questions);

        return new ArrayList<>(
                questions.subList(
                        0,
                        COMMON_ROUND_COUNT
                )
        );
    }

    private List<GameRound> createRounds(
            GameSession gameSession,
            List<Participant> participants,
            List<Question> personalQuestions,
            List<Question> commonQuestions
    ) {
        List<GameRound> rounds = new ArrayList<>();

        int roundOrder = 1;

        for (int index = 0;
             index < participants.size();
             index++) {

            rounds.add(
                    new GameRound(
                            gameSession,
                            personalQuestions.get(index),
                            participants.get(index),
                            roundOrder++
                    )
            );
        }

        for (Question commonQuestion : commonQuestions) {
            rounds.add(
                    new GameRound(
                            gameSession,
                            commonQuestion,
                            null,
                            roundOrder++
                    )
            );
        }

        return rounds;
    }

    private int getTimeLimitSeconds(
            QuestionType questionType
    ) {
        return switch (questionType) {
            case BLANK ->
                    BLANK_TIME_LIMIT_SECONDS;

            case INDIVIDUAL_CHOICE ->
                    INDIVIDUAL_CHOICE_TIME_LIMIT_SECONDS;

            case COMMON_VOTE ->
                    COMMON_VOTE_TIME_LIMIT_SECONDS;
        };
    }

    private RoundStartResponse createRoundStartResponse(
            GameRound gameRound,
            int timeLimitSeconds
    ) {
        Question question = gameRound.getQuestion();

        List<QuestionOptionResponse> options =
                question.getOptions()
                        .stream()
                        .map(option ->
                                new QuestionOptionResponse(
                                        option.getId(),
                                        option.getContent(),
                                        option.getDisplayOrder()
                                )
                        )
                        .toList();

        Long targetId =
                gameRound.getTargetParticipant() == null
                        ? null
                        : gameRound
                          .getTargetParticipant()
                          .getId();

        return new RoundStartResponse(
                gameRound.getId(),
                gameRound.getRoundOrder(),
                gameRound.getGameSession().getTotalRounds(),
                question.getQuestionType(),
                targetId,
                question.getContent(),
                options,
                timeLimitSeconds
        );
    }
}
