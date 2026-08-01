package com.impress.server.game.domain;

import com.impress.server.room.domain.Room;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "game_sessions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private GameSessionStatus status;

    @Column(name = "total_rounds", nullable = false)
    private int totalRounds;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    public GameSession(
            Room room,
            int totalRounds
    ) {
        if (totalRounds <= 0) {
            throw new IllegalArgumentException(
                    "전체 라운드 수는 1개 이상이어야 합니다."
            );
        }

        this.room = room;
        this.totalRounds = totalRounds;
        this.status = GameSessionStatus.PLAYING;
        this.startedAt = LocalDateTime.now();
    }

    public void finish() {
        this.status = GameSessionStatus.FINISHED;
        this.finishedAt = LocalDateTime.now();
    }
}