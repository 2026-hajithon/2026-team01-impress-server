package com.impress.server.room.service;

import com.impress.server.participant.domain.ConnectionStatus;
import com.impress.server.participant.domain.Participant;
import com.impress.server.participant.domain.ParticipantRole;
import com.impress.server.participant.repository.ParticipantRepository;
import com.impress.server.room.domain.Room;
import com.impress.server.room.domain.RoomStatus;
import com.impress.server.room.dto.*;
import com.impress.server.room.repository.RoomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final ParticipantRepository participantRepository;

    @Transactional
    public RoomCreateResponse createRoom(RoomCreateRequest request) {
        // 1. 중복되지 않는 4자리 방 코드 생성
        String roomCode = generateUniqueRoomCode();

        // 2. 방(Room) 엔티티 생성 및 저장
        Room room = new Room(request.getRoomName(), roomCode);
        Room savedRoom = roomRepository.save(room);

        // 3. 방장(Participant) 엔티티 생성 및 저장
        Participant host = new Participant(room, request.getHostName(), ParticipantRole.HOST);
        Participant savedHost = participantRepository.save(host);

        // 4. 응답 데이터 반환
        return new RoomCreateResponse(
                savedRoom.getCode(),
                savedHost.getId(),
                savedHost.getRole()
        );
    }

    // 4자리 랜덤 숫자 코드 생성 로직 (ex: "0482")
    private String generateUniqueRoomCode() {
        String code;
        do {
            // 수정: 멀티스레드 환경에서 더 빠르고 안전한 ThreadLocalRandom 사용
            int randomNum = ThreadLocalRandom.current().nextInt(10000);
            code = String.format("%04d", randomNum);
        } while (roomRepository.existsByCode(code));

        return code;
    }

    @Transactional
    public RoomJoinResponse joinRoom(String roomCode, RoomJoinRequest request) {
        // 1. 존재하는 방인지 검증
        Room room = roomRepository.findByCode(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방 코드입니다."));

        // 2. 방 상태가 WAITING(대기방)인지 검증
        if (room.getStatus() != RoomStatus.WAITING) {
            throw new IllegalStateException("이미 게임이 시작되었거나 종료된 방입니다.");
        }

        // 3. 중복된 이름인지 검증 (동일한 방 내에서만)
        if (participantRepository.existsByRoomAndName(room, request.getName())) {
            throw new IllegalArgumentException("방 안에 이미 동일한 닉네임이 존재합니다.");
        }

        // 4. 참가 가능 인원 초과 검증 로직 (예: 최대 8명)
        long currentParticipants = participantRepository.countByRoom(room);
        if (currentParticipants >= 8) {
             throw new IllegalStateException("방의 인원이 가득 찼습니다.");
         }

        // 5. 일반 유저(GUEST) 엔티티 생성 및 저장
        Participant guest = new Participant(room, request.getName(), ParticipantRole.GUEST);
        Participant savedGuest = participantRepository.save(guest);

        // 6. 응답 데이터 반환
        return new RoomJoinResponse(
                savedGuest.getId(),
                room.getStatus(),
                savedGuest.getRole()
        );
    }
    @Transactional
    public RoomSyncResponse syncRoomState(String roomCode, Long participantId) {
        // 1. 방 조회
        Room room = roomRepository.findByCode(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));

        // 2. 요청한 참가자 조회 및 해당 방 소속이 맞는지 검증
        Participant me = participantRepository.findById(participantId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 참가자입니다."));

        if (!me.getRoom().getId().equals(room.getId())) {
            throw new IllegalArgumentException("해당 방의 참가자가 아닙니다.");
        }

        // 3. 응답 객체 뼈대 생성 (공통 필드 세팅)
        RoomSyncResponse.RoomSyncResponseBuilder responseBuilder = RoomSyncResponse.builder()
                .roomStatus(room.getStatus())
                .myRole(me.getRole());

        // 4. 방 상태에 따른 추가 데이터 세팅
        switch (room.getStatus()) {
            case WAITING:
                // 대기방: 현재 방에 있는 모든 참가자 목록을 조회하여 DTO로 변환
                List<ParticipantInfo> participantInfos = participantRepository.findByRoom(room).stream()
                        .map(p -> new ParticipantInfo(
                                p.getId(),
                                p.getName(),
                                p.getRole(),
                                p.getConnectionStatus()
                        ))
                        .collect(Collectors.toList());
                responseBuilder.participants(participantInfos);
                break;

            case PLAYING:
                // TODO: 게임 진행 상태
                // 차후 game_sessions, game_rounds 테이블이 생성되면 여기서 현재 라운드를 조회해 세팅합니다.
                // responseBuilder.currentRound(조회된_라운드_정보);
                break;

            case FINISHED:
                // 종료 상태: 공통 필드(roomStatus, myRole)만 있으면 되므로 추가 작업 없음
                break;
        }

        return responseBuilder.build();
    }
}