package com.impress.server.room.controller;

import com.impress.server.common.dto.ApiResponse;
import com.impress.server.room.dto.*;
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

    @GetMapping("/{roomCode}/sync")
    public ResponseEntity<ApiResponse<RoomSyncResponse>> syncRoom(
            @PathVariable String roomCode,
            @RequestHeader("Participant-Id") Long participantId) {

        RoomSyncResponse responseData = roomService.syncRoomState(roomCode, participantId);

        return ResponseEntity.ok(ApiResponse.success(responseData));
    }

    @DeleteMapping("/{roomCode}/participants/me")
    public ResponseEntity<ApiResponse<Void>> leaveRoom(
            @PathVariable String roomCode,
            @RequestHeader("Participant-Id") Long participantId) {

        // 서비스 로직 실행 (검증 및 DB 삭제)
        roomService.leaveRoom(roomCode, participantId);

        // 데이터가 없는 공통 성공 응답 반환
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/{roomCode}/result")
    public ResponseEntity<ApiResponse<GameResultResponse>> getGameResult(
            @PathVariable String roomCode,
            @RequestHeader("Participant-Id") Long participantId) {

        GameResultResponse responseData = roomService.getGameResult(roomCode, participantId);

        return ResponseEntity.ok(ApiResponse.success(responseData));
    }

    @GetMapping("/{roomCode}/host")
    public ResponseEntity<ApiResponse<RoomHostResponse>> getRoomHost(@PathVariable String roomCode) {
        RoomHostResponse response = roomService.getRoomHost(roomCode);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{roomCode}/name")
    public ResponseEntity<ApiResponse<RoomNameResponse>> getRoomName(@PathVariable String roomCode) {
        RoomNameResponse response = roomService.getRoomName(roomCode);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
