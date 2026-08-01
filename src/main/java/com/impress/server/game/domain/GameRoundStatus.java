package com.impress.server.game.domain;

public enum GameRoundStatus {

    PENDING, // 아직 시작 전
    ANSWERING, // 참가자가 답변 중
    RESULT, // 결과 화면을 보고 다음 진행 투표 중
    COMPLETED // 다음 라운드로 넘어간 완료 상태
}