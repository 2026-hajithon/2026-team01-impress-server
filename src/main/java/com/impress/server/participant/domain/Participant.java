package com.impress.server.participant.domain;

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
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "participants")
public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "room_id",
            nullable = false
    )
    private Room room;

    @Column(nullable = false, length = 20)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ParticipantRole role;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "connection_status",
            nullable = false,
            length = 20
    )
    private ConnectionStatus connectionStatus;

    @Column(
            name = "joined_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime joinedAt;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    public Participant(
            Room room,
            String name,
            ParticipantRole role
    ) {
        this.room = room;
        this.name = name;
        this.role = role;
        this.connectionStatus = ConnectionStatus.DISCONNECTED;
        this.joinedAt = LocalDateTime.now();
    }

    public void connect() {
        this.connectionStatus = ConnectionStatus.CONNECTED;
    }

    public void disconnect() {
        this.connectionStatus = ConnectionStatus.DISCONNECTED;
        this.lastSeenAt = LocalDateTime.now();
    }
}