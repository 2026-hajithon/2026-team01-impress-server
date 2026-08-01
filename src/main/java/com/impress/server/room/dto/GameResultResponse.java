package com.impress.server.room.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class GameResultResponse {
    private String roomCode;
    private String roomName;
    private Long gameSessionId;
    private List<ParticipantInfo> participants; // 이전에 만든 ParticipantInfo 재사용
    private List<RoundResultDto> rounds;

    @Getter
    @Builder
    public static class RoundResultDto {
        private Long roundId;
        private Integer roundOrder;
        private String qType;
        private Long targetId;
        private String targetName;
        private String question;
        private Map<String, Object> result; // 질문 유형마다 구조가 달라 Map 활용
    }
}