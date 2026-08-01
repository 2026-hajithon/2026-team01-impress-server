package com.impress.server.game.repository;

import com.impress.server.game.domain.GameRound;
import com.impress.server.game.domain.GameRoundStatus;
import com.impress.server.game.domain.GameSession;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GameRoundRepository
        extends JpaRepository<GameRound, Long> {

    List<GameRound> findAllByGameSessionOrderByRoundOrderAsc(
            GameSession gameSession
    );

    Optional<GameRound> findByGameSessionAndRoundOrder(
            GameSession gameSession,
            int roundOrder
    );

    Optional<GameRound>
    findFirstByGameSessionAndStatusInOrderByRoundOrderAsc(
            GameSession gameSession,
            Collection<GameRoundStatus> statuses
    );

    long countByGameSession(
            GameSession gameSession
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT gameRound
        FROM GameRound gameRound
        WHERE gameRound.id = :roundId
        """)
    Optional<GameRound> findByIdForUpdate(
            @Param("roundId") Long roundId
    );
}