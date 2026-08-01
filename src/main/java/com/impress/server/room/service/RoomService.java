package com.impress.server.room.service;

import com.impress.server.participant.domain.ConnectionStatus;
import com.impress.server.participant.domain.Participant;
import com.impress.server.participant.domain.ParticipantRole;
import com.impress.server.participant.repository.ParticipantRepository;
import com.impress.server.room.domain.Room;
import com.impress.server.room.domain.RoomStatus;
import com.impress.server.room.dto.RoomCreateRequest;
import com.impress.server.room.dto.RoomCreateResponse;
import com.impress.server.room.dto.RoomJoinRequest;
import com.impress.server.room.dto.RoomJoinResponse;
import com.impress.server.room.repository.RoomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

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
}