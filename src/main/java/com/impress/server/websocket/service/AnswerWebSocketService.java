package com.impress.server.websocket.service;

import com.impress.server.answer.domain.Answer;
import com.impress.server.answer.repository.AnswerRepository;
import com.impress.server.game.domain.GameRound;
import com.impress.server.game.domain.GameRoundStatus;
import com.impress.server.game.domain.GameSessionStatus;
import com.impress.server.game.repository.GameRoundRepository;
import com.impress.server.participant.domain.Participant;
import com.impress.server.participant.repository.ParticipantRepository;
import com.impress.server.question.domain.QuestionOption;
import com.impress.server.question.domain.QuestionType;
import com.impress.server.question.repository.QuestionOptionRepository;
import com.impress.server.room.domain.Room;
import com.impress.server.room.domain.RoomStatus;
import com.impress.server.room.repository.RoomRepository;
import com.impress.server.websocket.dto.request.AnswerSubmitRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AnswerWebSocketService {

    private static final int MAX_TEXT_ANSWER_LENGTH = 255;

    private final RoomRepository roomRepository;
    private final ParticipantRepository participantRepository;
    private final GameRoundRepository gameRoundRepository;
    private final AnswerRepository answerRepository;
    private final QuestionOptionRepository questionOptionRepository;

    @Transactional
    public void submit(
            String roomCode,
            Long participantId,
            AnswerSubmitRequest request
    ) {
        validateRequest(request);

        Room room = roomRepository.findByCode(roomCode)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 방입니다."
                        )
                );

        if (room.getStatus() != RoomStatus.PLAYING) {
            throw new IllegalStateException(
                    "게임이 진행 중인 방이 아닙니다."
            );
        }

        Participant participant =
                participantRepository.findByIdAndRoom(
                        participantId,
                        room
                ).orElseThrow(() ->
                        new IllegalArgumentException(
                                "해당 방의 참가자가 아닙니다."
                        )
                );

        GameRound gameRound =
                gameRoundRepository.findById(
                        request.roundId()
                ).orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 라운드입니다."
                        )
                );

        validateCurrentRound(gameRound, room);
        validateDeadline(gameRound);
        validateDuplicateAnswer(gameRound, participant);

        Answer answer = createAnswer(
                gameRound,
                participant,
                room,
                request
        );

        answerRepository.saveAndFlush(answer);
    }

    private void validateRequest(
            AnswerSubmitRequest request
    ) {
        if (request == null
                || request.roundId() == null
                || request.roundId() <= 0) {
            throw new IllegalArgumentException(
                    "올바른 라운드 ID가 필요합니다."
            );
        }
    }

    private void validateCurrentRound(
            GameRound gameRound,
            Room room
    ) {
        Room roundRoom =
                gameRound.getGameSession().getRoom();

        if (!roundRoom.getId().equals(room.getId())) {
            throw new IllegalArgumentException(
                    "해당 방의 라운드가 아닙니다."
            );
        }

        if (gameRound.getGameSession().getStatus()
                != GameSessionStatus.PLAYING) {
            throw new IllegalStateException(
                    "현재 진행 중인 게임 세션이 아닙니다."
            );
        }

        if (gameRound.getStatus()
                != GameRoundStatus.ANSWERING) {
            throw new IllegalStateException(
                    "현재 답변할 수 있는 라운드가 아닙니다."
            );
        }
    }

    private void validateDeadline(
            GameRound gameRound
    ) {
        LocalDateTime deadlineAt =
                gameRound.getDeadlineAt();

        if (deadlineAt == null
                || !LocalDateTime.now().isBefore(deadlineAt)) {
            throw new IllegalStateException(
                    "답변 시간이 종료되었습니다."
            );
        }
    }

    private void validateDuplicateAnswer(
            GameRound gameRound,
            Participant participant
    ) {
        if (answerRepository
                .existsByGameRoundAndRespondentParticipant(
                        gameRound,
                        participant
                )) {
            throw new IllegalStateException(
                    "이미 답변을 제출했습니다."
            );
        }
    }

    private Answer createAnswer(
            GameRound gameRound,
            Participant participant,
            Room room,
            AnswerSubmitRequest request
    ) {
        QuestionType questionType =
                gameRound.getQuestion().getQuestionType();

        return switch (questionType) {
            case BLANK ->
                    createBlankAnswer(
                            gameRound,
                            participant,
                            request
                    );

            case INDIVIDUAL_CHOICE ->
                    createChoiceAnswer(
                            gameRound,
                            participant,
                            request
                    );

            case COMMON_VOTE ->
                    createCommonVoteAnswer(
                            gameRound,
                            participant,
                            room,
                            request
                    );
        };
    }

    private Answer createBlankAnswer(
            GameRound gameRound,
            Participant participant,
            AnswerSubmitRequest request
    ) {
        if (request.selectedOptionId() != null
                || request.pickedParticipantId() != null) {
            throw new IllegalArgumentException(
                    "빈칸 질문에 사용할 수 없는 답변 필드입니다."
            );
        }

        if (gameRound.getTargetParticipant() != null
                && gameRound.getTargetParticipant()
                .getId()
                .equals(participant.getId())) {
            throw new IllegalStateException(
                    "질문의 대상자는 빈칸 답변을 제출할 수 없습니다."
            );
        }

        String textAnswer = request.textAnswer();

        if (textAnswer == null || textAnswer.isBlank()) {
            throw new IllegalArgumentException(
                    "빈칸 답변을 입력해야 합니다."
            );
        }

        String trimmedAnswer = textAnswer.trim();

        if (trimmedAnswer.length()
                > MAX_TEXT_ANSWER_LENGTH) {
            throw new IllegalArgumentException(
                    "빈칸 답변은 255자 이하로 입력해야 합니다."
            );
        }

        return Answer.submitBlank(
                gameRound,
                participant,
                trimmedAnswer
        );
    }

    private Answer createChoiceAnswer(
            GameRound gameRound,
            Participant participant,
            AnswerSubmitRequest request
    ) {
        if (request.textAnswer() != null
                || request.pickedParticipantId() != null) {
            throw new IllegalArgumentException(
                    "개인 선택형 질문에 사용할 수 없는 답변 필드입니다."
            );
        }

        if (request.selectedOptionId() == null
                || request.selectedOptionId() <= 0) {
            throw new IllegalArgumentException(
                    "선택지를 선택해야 합니다."
            );
        }

        QuestionOption selectedOption =
                questionOptionRepository
                        .findByIdAndQuestion(
                                request.selectedOptionId(),
                                gameRound.getQuestion()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "현재 질문에 속한 선택지가 아닙니다."
                                )
                        );

        return Answer.submitChoice(
                gameRound,
                participant,
                selectedOption
        );
    }

    private Answer createCommonVoteAnswer(
            GameRound gameRound,
            Participant participant,
            Room room,
            AnswerSubmitRequest request
    ) {
        if (request.textAnswer() != null
                || request.selectedOptionId() != null) {
            throw new IllegalArgumentException(
                    "공통 투표에 사용할 수 없는 답변 필드입니다."
            );
        }

        if (request.pickedParticipantId() == null
                || request.pickedParticipantId() <= 0) {
            throw new IllegalArgumentException(
                    "투표할 참가자를 선택해야 합니다."
            );
        }

        Participant selectedParticipant =
                participantRepository.findByIdAndRoom(
                        request.pickedParticipantId(),
                        room
                ).orElseThrow(() ->
                        new IllegalArgumentException(
                                "해당 방에 속한 참가자가 아닙니다."
                        )
                );

        return Answer.submitCommonVote(
                gameRound,
                participant,
                selectedParticipant
        );
    }
}