package com.impress.server.room.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RoomCreateRequest {
    private String hostName;
    private String roomName;
}