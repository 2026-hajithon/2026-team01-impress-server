package com.impress.server.game.repository;

import com.impress.server.game.domain.GameRound;
import com.impress.server.game.domain.GameRoundStatus;
import com.impress.server.game.domain.GameSession;
import org.springframework.data.jpa.repository.JpaRepository;

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

    Optional<GameRound> findTopByGameSessionAndStatusNotOrderByRoundOrderAsc(GameSession session, GameRoundStatus status);

    List<GameRound> findByGameSessionOrderByRoundOrderAsc(GameSession session);
}