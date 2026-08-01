package com.impress.server.room.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.impress.server.participant.domain.ConnectionStatus;
import com.impress.server.participant.domain.ParticipantRole;
import com.impress.server.room.domain.RoomStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
// null인 필드는 JSON 응답에서 아예 빼버리는 옵션입니다.
// 이를 통해 방 상태에 따라 필요한 필드만 깔끔하게 내려줄 수 있습니다.
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoomSyncResponse {

    // 공통 필드
    private RoomStatus roomStatus;
    private ParticipantRole myRole;

    // WAITING 상태일 때만 값이 들어가는 필드
    private List<ParticipantInfo> participants;

    // PLAYING 상태일 때만 값이 들어가는 필드 (추후 라운드 엔티티 구현 시 사용)
    private Object currentRound;

}