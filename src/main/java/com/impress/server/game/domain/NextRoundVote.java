package com.impress.server.game.domain;

import com.impress.server.participant.domain.Participant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "next_round_votes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NextRoundVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_round_id", nullable = false)
    private GameRound gameRound;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private Participant participant;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public NextRoundVote(
            GameRound gameRound,
            Participant participant
    ) {
        this.gameRound = gameRound;
        this.participant = participant;
        this.createdAt = LocalDateTime.now();
    }
}