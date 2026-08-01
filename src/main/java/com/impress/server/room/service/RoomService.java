package com.impress.server.room.service;

import com.impress.server.answer.repository.AnswerRepository;
import com.impress.server.game.domain.GameRound;
import com.impress.server.game.domain.GameRoundStatus;
import com.impress.server.game.domain.GameSession;
import com.impress.server.game.repository.GameRoundRepository;
import com.impress.server.game.repository.GameSessionRepository;
import com.impress.server.game.repository.NextRoundVoteRepository;
import com.impress.server.participant.domain.ConnectionStatus;
import com.impress.server.participant.domain.Participant;
import com.impress.server.participant.domain.ParticipantRole;
import com.impress.server.participant.repository.ParticipantRepository;
import com.impress.server.question.domain.Question;
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
    private final GameSessionRepository gameSessionRepository;
    private final NextRoundVoteRepository nextRoundVoteRepository;
    private final GameRoundRepository gameRoundRepository;
    private final AnswerRepository answerRepository;

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
                // 1. 현재 진행 중인 게임 세션 조회
                GameSession activeSession = gameSessionRepository.findByRoomAndStatus(room, "PLAYING")
                        .orElseThrow(() -> new IllegalStateException("진행 중인 게임 세션이 없습니다."));

                // 2. 현재 진행 중인 라운드 조회 (String "COMPLETED" 대신 Enum 사용)
                GameRound currentRound = gameRoundRepository.findTopByGameSessionAndStatusNotOrderByRoundOrderAsc(activeSession, GameRoundStatus.COMPLETED)
                        .orElseThrow(() -> new IllegalStateException("진행 중인 라운드가 없습니다."));

                Question question = currentRound.getQuestion();

                // [수정 포인트] String이 아니라 팀원이 만든 Enum 타입으로 받습니다.
                GameRoundStatus phase = currentRound.getStatus();

                // 3. 라운드 공통 정보 세팅
                CurrentRoundInfo.CurrentRoundInfoBuilder roundBuilder = CurrentRoundInfo.builder()
                        .roundId(currentRound.getId())
                        .roundOrder(currentRound.getRoundOrder())
                        .totalRounds(activeSession.getTotalRounds())
                        // [수정 포인트] Enum.name()을 사용해 DTO의 String 필드에 맞게 변환합니다.
                        .qType(question.getQuestionType().name())
                        .phase(phase.name())
                        .question(question.getContent())
                        .targetId(currentRound.getTargetParticipant() != null ? currentRound.getTargetParticipant().getId() : null);

                // 4. 라운드 Phase(상태)에 따른 분기 처리 (equals 대신 Enum 비교 == 사용)
                if (phase == GameRoundStatus.ANSWERING) {
                    int timeRemaining = 0;
                    if (currentRound.getDeadlineAt() != null) {
                        timeRemaining = (int) java.time.Duration.between(java.time.LocalDateTime.now(), currentRound.getDeadlineAt()).getSeconds();
                        timeRemaining = Math.max(0, timeRemaining);
                    }

                    boolean myAnswerSubmitted = answerRepository.existsByGameRoundIdAndRespondentParticipantId(currentRound.getId(), participantId);

                    roundBuilder.timeRemaining(timeRemaining)
                            .myAnswerSubmitted(myAnswerSubmitted);

                } else if (phase == GameRoundStatus.RESULT) {
                    boolean myNextVoteSubmitted = nextRoundVoteRepository.existsByGameRoundIdAndParticipantId(currentRound.getId(), participantId);

                    long nextVoteCount = nextRoundVoteRepository.countByGameRoundId(currentRound.getId());

                    long connectedCount = participantRepository.countByRoomAndConnectionStatus(room, ConnectionStatus.CONNECTED);
                    int nextVoteRequired = (int) Math.floor(connectedCount / 2.0) + 1;

                    // TODO: 결과 조회 로직

                    roundBuilder.myNextVoteSubmitted(myNextVoteSubmitted)
                            .nextVoteCount((int) nextVoteCount)
                            .nextVoteRequired(nextVoteRequired);
                }

                responseBuilder.currentRound(roundBuilder.build());
                break;

            case FINISHED:
                // 종료 상태: 공통 필드(roomStatus, myRole)만 있으면 되므로 추가 작업 없음
                break;
        }

        return responseBuilder.build();
    }

    @Transactional
    public void leaveRoom(String roomCode, Long participantId) {
        // 1. 방 조회
        Room room = roomRepository.findByCode(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));

        // 2. 참가자 조회
        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 참가자입니다."));

        // 3. 검증: 요청한 참가자가 해당 방에 속해있는지 확인
        if (!participant.getRoom().getId().equals(room.getId())) {
            throw new IllegalArgumentException("해당 방의 참가자가 아닙니다.");
        }

        // 4. 검증: 방 상태가 WAITING(대기방)인지 확인
        if (room.getStatus() != RoomStatus.WAITING) {
            throw new IllegalStateException("게임이 이미 시작되어 나갈 수 없습니다.");
        }

        // 5. 역할에 따른 분기 처리 (방 폭파 vs 단순 퇴장)
        if (participant.getRole() == ParticipantRole.HOST) {
            // [방장인 경우] 방 폭파
            participantRepository.deleteByRoom(room);
            roomRepository.delete(room);
        } else {
            // [일반 참가자인 경우] 본인만 방에서 나가기
            participantRepository.delete(participant);
        }
    }
}