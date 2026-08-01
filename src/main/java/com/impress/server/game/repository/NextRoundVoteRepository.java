package com.impress.server.game.repository;

import com.impress.server.game.domain.GameRound;
import com.impress.server.game.domain.NextRoundVote;
import com.impress.server.participant.domain.ConnectionStatus;
import com.impress.server.participant.domain.Participant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NextRoundVoteRepository
        extends JpaRepository<NextRoundVote, Long> {

    boolean existsByGameRoundAndParticipant(
            GameRound gameRound,
            Participant participant
    );

    long countByGameRound(
            GameRound gameRound
    );

    long countByGameRoundAndParticipant_ConnectionStatus(
            GameRound gameRound,
            ConnectionStatus connectionStatus
    );

    List<NextRoundVote> findAllByGameRound(
            GameRound gameRound
    );

    boolean existsByGameRoundIdAndParticipantId(Long roundId, Long participantId);
    long countByGameRoundId(Long roundId);
}