package com.impress.server.game.repository;

import com.impress.server.game.domain.GameRound;
import com.impress.server.game.domain.GameSession;
import com.impress.server.game.domain.GameSessionStatus;
import com.impress.server.room.domain.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameSessionRepository
        extends JpaRepository<GameSession, Long> {

    boolean existsByRoomAndStatus(
            Room room,
            GameSessionStatus status
    );

    Optional<GameSession> findFirstByRoomAndStatusOrderByIdDesc(
            Room room,
            GameSessionStatus status
    );

    Optional<GameSession> findByRoomAndStatus(Room room, GameSessionStatus status);

    Optional<GameSession> findTopByRoomAndStatusOrderByIdDesc(Room room, GameSessionStatus status);

}