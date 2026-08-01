package com.impress.server.game.domain;

import com.impress.server.participant.domain.Participant;
import com.impress.server.question.domain.Question;
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
@Table(name = "game_rounds")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_session_id", nullable = false)
    private GameSession gameSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_participant_id")
    private Participant targetParticipant;

    @Column(name = "round_order", nullable = false)
    private int roundOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private GameRoundStatus status;

    @Column(name = "deadline_at")
    private LocalDateTime deadlineAt;

    public GameRound(
            GameSession gameSession,
            Question question,
            Participant targetParticipant,
            int roundOrder
    ) {
        if (roundOrder <= 0) {
            throw new IllegalArgumentException(
                    "라운드 순서는 1부터 시작해야 합니다."
            );
        }

        this.gameSession = gameSession;
        this.question = question;
        this.targetParticipant = targetParticipant;
        this.roundOrder = roundOrder;
        this.status = GameRoundStatus.PENDING;
    }

    public void start(LocalDateTime deadlineAt) {
        this.status = GameRoundStatus.ANSWERING;
        this.deadlineAt = deadlineAt;
    }

    public void openResult() {
        this.status = GameRoundStatus.RESULT;
        this.deadlineAt = null;
    }

    public void complete() {
        this.status = GameRoundStatus.COMPLETED;
        this.deadlineAt = null;
    }
}