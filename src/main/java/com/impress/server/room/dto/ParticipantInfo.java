package com.impress.server.room.dto;

import com.impress.server.participant.domain.ConnectionStatus;
import com.impress.server.participant.domain.ParticipantRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ParticipantInfo {
    private Long participantId;
    private String name;
    private ParticipantRole role;
    private ConnectionStatus connectionStatus;
}