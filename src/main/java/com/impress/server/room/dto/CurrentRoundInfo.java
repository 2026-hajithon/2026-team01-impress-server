package com.impress.server.room.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // null인 필드는 JSON 응답에서 제외
public class CurrentRoundInfo {
    private Long roundId;
    private Integer roundOrder;
    private Integer totalRounds;
    private String qType;
    private String phase; // ANSWERING, RESULT 등
    private String question;
    private Long targetId;

    // phase가 ANSWERING일 때만 세팅되는 필드
    private Integer timeRemaining;
    private Boolean myAnswerSubmitted;

    // phase가 RESULT일 때만 세팅되는 필드
    private Boolean myNextVoteSubmitted;
    private Integer nextVoteCount;
    private Integer nextVoteRequired;
    private Object result; // 질문 유형별 결과 (추후 별도 클래스로 구체화)
}