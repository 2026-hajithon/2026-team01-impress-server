package com.impress.server.answer.domain;

import com.impress.server.game.domain.GameRound;
import com.impress.server.participant.domain.Participant;
import com.impress.server.question.domain.QuestionOption;
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
@Table(name = "answers")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_round_id", nullable = false)
    private GameRound gameRound;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "respondent_participant_id", nullable = false)
    private Participant respondentParticipant;

    @Column(name = "text_content", length = 255)
    private String textContent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_option_id")
    private QuestionOption selectedOption;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_participant_id")
    private Participant selectedParticipant;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    private Answer(
            GameRound gameRound,
            Participant respondentParticipant
    ) {
        this.gameRound = gameRound;
        this.respondentParticipant = respondentParticipant;
        this.submittedAt = LocalDateTime.now();
    }

    public static Answer submitBlank(
            GameRound gameRound,
            Participant respondentParticipant,
            String textContent
    ) {
        if (textContent == null || textContent.isBlank()) {
            throw new IllegalArgumentException(
                    "빈칸 답변을 입력해야 합니다."
            );
        }

        Answer answer = new Answer(
                gameRound,
                respondentParticipant
        );
        answer.textContent = textContent;

        return answer;
    }

    public static Answer submitChoice(
            GameRound gameRound,
            Participant respondentParticipant,
            QuestionOption selectedOption
    ) {
        if (selectedOption == null) {
            throw new IllegalArgumentException(
                    "선택지를 선택해야 합니다."
            );
        }

        Answer answer = new Answer(
                gameRound,
                respondentParticipant
        );
        answer.selectedOption = selectedOption;

        return answer;
    }

    public static Answer submitCommonVote(
            GameRound gameRound,
            Participant respondentParticipant,
            Participant selectedParticipant
    ) {
        if (selectedParticipant == null) {
            throw new IllegalArgumentException(
                    "참가자를 선택해야 합니다."
            );
        }

        Answer answer = new Answer(
                gameRound,
                respondentParticipant
        );
        answer.selectedParticipant = selectedParticipant;

        return answer;
    }
}