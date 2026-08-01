package com.impress.server.room.controller;

import com.impress.server.common.dto.ApiResponse;
import com.impress.server.room.dto.RoomCreateRequest;
import com.impress.server.room.dto.RoomCreateResponse;
import com.impress.server.room.dto.RoomJoinRequest;
import com.impress.server.room.dto.RoomJoinResponse;
import com.impress.server.room.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<ApiResponse<RoomCreateResponse>> createRoom(@RequestBody RoomCreateRequest request) {
        RoomCreateResponse responseData = roomService.createRoom(request);

        // 공통 응답 객체로 감싸서 반환
        return ResponseEntity.ok(ApiResponse.success(responseData));
    }

    @PostMapping("/{roomCode}/join")
    public ResponseEntity<ApiResponse<RoomJoinResponse>> joinRoom(
            @PathVariable String roomCode,
            @RequestBody RoomJoinRequest request) {

        RoomJoinResponse responseData = roomService.joinRoom(roomCode, request);

        // 공통 응답 객체로 감싸서 반환
        return ResponseEntity.ok(ApiResponse.success(responseData));
    }


}
