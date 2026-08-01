package com.impress.server.room.dto;

import com.impress.server.participant.domain.ParticipantRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RoomCreateResponse {
    private String roomCode;
    private Long participantId;
    private ParticipantRole role;
}