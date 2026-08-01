package com.impress.server.room.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(
            nullable = false,
            unique = true,
            columnDefinition = "CHAR(4)"
    )
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomStatus status;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    public Room(
            String name,
            String code
    ) {
        this.name = name;
        this.code = code;
        this.status = RoomStatus.WAITING;
        this.createdAt = LocalDateTime.now();
    }

    public void startGame() {
        if (status != RoomStatus.WAITING
                && status != RoomStatus.FINISHED) {
            throw new IllegalStateException(
                    "현재 방 상태에서는 게임을 시작할 수 없습니다."
            );
        }

        this.status = RoomStatus.PLAYING;
    }

    public void finishGame() {
        if (status != RoomStatus.PLAYING) {
            throw new IllegalStateException(
                    "진행 중인 방만 종료할 수 있습니다."
            );
        }

        this.status = RoomStatus.FINISHED;
    }
}