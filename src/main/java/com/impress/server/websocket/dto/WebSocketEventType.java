package com.impress.server.websocket.dto;

// 서버가 어떤 종류의 알림을 보냈는지 표시하는 라벨 목록
public enum WebSocketEventType {

    PARTICIPANT_LIST_UPDATE, // 입장, 퇴장, 강퇴, 연결 상태 변경
    PARTICIPANT_KICKED,      // 특정 참가자 강제 퇴장
    ROUND_START, // 새로운 라운드 문제 전달
    ROUND_RESULT, // 현재 라운드 결과
    NEXT_ROUND_VOTE_UPDATE,  // 다음 라운드 진행 동의 현황
    GAME_END, // 모든 라운드 종료
    ERROR // 요청 실패
}
