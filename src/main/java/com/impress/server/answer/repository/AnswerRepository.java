package com.impress.server.answer.repository;

import com.impress.server.answer.domain.Answer;
import com.impress.server.game.domain.GameRound;
import com.impress.server.participant.domain.Participant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnswerRepository
        extends JpaRepository<Answer, Long> {

    boolean existsByGameRoundAndRespondentParticipant(
            GameRound gameRound,
            Participant respondentParticipant
    );

    Optional<Answer> findByGameRoundAndRespondentParticipant(
            GameRound gameRound,
            Participant respondentParticipant
    );

    List<Answer> findAllByGameRoundOrderByIdAsc(
            GameRound gameRound
    );

    long countByGameRound(
            GameRound gameRound
    );

    boolean existsByGameRoundIdAndRespondentParticipantId(Long roundId, Long participantId);

    List<Answer> findByGameRound(GameRound round);
}