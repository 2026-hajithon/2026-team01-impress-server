package com.impress.server.room.dto;

import com.impress.server.participant.domain.ParticipantRole;
import com.impress.server.room.domain.RoomStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RoomJoinResponse {
    private Long participantId;
    private RoomStatus roomStatus;
    private ParticipantRole role;
}