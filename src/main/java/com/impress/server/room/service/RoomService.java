package com.impress.server.room.service;

import com.impress.server.answer.domain.Answer;
import com.impress.server.answer.repository.AnswerRepository;
import com.impress.server.game.domain.GameRound;
import com.impress.server.game.domain.GameRoundStatus;
import com.impress.server.game.domain.GameSession;
import com.impress.server.game.domain.GameSessionStatus;
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
import java.util.Map;
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
                GameSession activeSession = gameSessionRepository.findByRoomAndStatus(room, GameSessionStatus.PLAYING)
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

                    List<Answer> answers = answerRepository.findByGameRound(currentRound);
                    Map<String, Object> resultData = buildResultMap(currentRound, answers);

                    roundBuilder.myNextVoteSubmitted(myNextVoteSubmitted)
                            .nextVoteCount((int) nextVoteCount)
                            .nextVoteRequired(nextVoteRequired)
                            .result(resultData);
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

    @Transactional
    public GameResultResponse getGameResult(String roomCode, Long participantId) {
        // 1. 방 및 참가자 검증
        Room room = roomRepository.findByCode(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));
        Participant me = participantRepository.findById(participantId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 참가자입니다."));
        if (!me.getRoom().getId().equals(room.getId())) {
            throw new IllegalArgumentException("해당 방의 참가자가 아닙니다.");
        }

        // 2. 가장 최근에 종료된 게임 세션 조회
        GameSession lastSession = gameSessionRepository.findTopByRoomAndStatusOrderByIdDesc(room, GameSessionStatus.FINISHED)
                .orElseThrow(() -> new IllegalStateException("종료된 게임 세션이 없습니다."));

        // 3. 참가자 목록 매핑
        List<ParticipantInfo> participantInfos = participantRepository.findByRoom(room).stream()
                .map(p -> new ParticipantInfo(
                        p.getId(), p.getName(), p.getRole(), p.getConnectionStatus()
                )).collect(Collectors.toList());

        // 4. 라운드 및 답변 결과 매핑
        List<GameRound> rounds = gameRoundRepository.findByGameSessionOrderByRoundOrderAsc(lastSession);
        List<GameResultResponse.RoundResultDto> roundDtos = rounds.stream().map(round -> {
            Question question = round.getQuestion();
            Participant target = round.getTargetParticipant();

            // 해당 라운드의 모든 답변 조회
            List<Answer> answers = answerRepository.findByGameRound(round);

            // qType에 따른 result 데이터 생성
            Map<String, Object> resultData = buildResultMap(round, answers);

            return GameResultResponse.RoundResultDto.builder()
                    .roundId(round.getId())
                    .roundOrder(round.getRoundOrder())
                    .qType(question.getQuestionType().name())
                    .targetId(target != null ? target.getId() : null)
                    .targetName(target != null ? target.getName() : null)
                    .question(question.getContent())
                    .result(resultData)
                    .build();
        }).collect(Collectors.toList());

        // 5. 최종 응답 반환
        return GameResultResponse.builder()
                .roomCode(room.getCode())
                .roomName(room.getName())
                .gameSessionId(lastSession.getId())
                .participants(participantInfos)
                .rounds(roundDtos)
                .build();
    }

    // 💡 질문 유형별로 답변 결과를 예쁘게 포맷팅하는 헬퍼 메서드
    private Map<String, Object> buildResultMap(GameRound round, List<Answer> answers) {
        // 엔티티 구조에 따라 QuestionType Enum의 이름을 가져옵니다.
        // DB 스키마에는 INDIVIDUAL_CHOICE로 되어 있고 API 명세에는 INDIVIDUAL_OX로 되어있어 둘 다 호환되게 처리합니다.
        String qType = round.getQuestion().getQuestionType().name();

        // 1. 빈칸 질문 (BLANK)
        if ("BLANK".equals(qType)) {
            List<Map<String, Object>> answerList = answers.stream().map(a -> {
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("submitterId", a.getRespondentParticipant().getId());
                map.put("submitterName", a.getRespondentParticipant().getName());
                map.put("textAnswer", a.getTextContent()); // answers.text_content 사용
                return map;
            }).collect(Collectors.toList());

            return Map.of("answers", answerList);
        }

        // 2. 개인 OX 질문 (INDIVIDUAL_OX / INDIVIDUAL_CHOICE)
        if ("INDIVIDUAL_OX".equals(qType) || "INDIVIDUAL_CHOICE".equals(qType)) {
            List<Map<String, Object>> correctSubmitters = new java.util.ArrayList<>();
            List<Map<String, Object>> wrongSubmitters = new java.util.ArrayList<>();
            String trueAnswer = "";

            Long targetId = round.getTargetParticipant().getId();

            // 대상자(Target)의 제출 답변을 찾아 실제 정답(trueAnswer)으로 간주
            Answer targetAnswer = answers.stream()
                    .filter(a -> a.getRespondentParticipant().getId().equals(targetId))
                    .findFirst()
                    .orElse(null);

            if (targetAnswer != null && targetAnswer.getSelectedOption() != null) {
                trueAnswer = targetAnswer.getSelectedOption().getContent();
                Long trueOptionId = targetAnswer.getSelectedOption().getId();

                for (Answer a : answers) {
                    // 대상자 본인의 답변은 채점자 목록에서 제외
                    if (a.getRespondentParticipant().getId().equals(targetId)) continue;

                    Map<String, Object> submitter = new java.util.HashMap<>();
                    submitter.put("submitterId", a.getRespondentParticipant().getId());
                    submitter.put("submitterName", a.getRespondentParticipant().getName());

                    // 선택한 옵션 ID가 정답 옵션 ID와 같으면 정답자, 아니면 오답자
                    if (a.getSelectedOption() != null && a.getSelectedOption().getId().equals(trueOptionId)) {
                        correctSubmitters.add(submitter);
                    } else {
                        wrongSubmitters.add(submitter);
                    }
                }
            }

            return Map.of(
                    "trueAnswer", trueAnswer,
                    "correctSubmitters", correctSubmitters,
                    "wrongSubmitters", wrongSubmitters
            );
        }

        // 3. 공통 투표 (COMMON_VOTE)
        if ("COMMON_VOTE".equals(qType)) {
            // answers 테이블의 selected_participant_id를 기준으로 그룹화하여 투표 수(Count) 계산
            Map<Participant, Long> voteCounts = answers.stream()
                    .filter(a -> a.getSelectedParticipant() != null)
                    .collect(Collectors.groupingBy(Answer::getSelectedParticipant, Collectors.counting()));

            List<Map<String, Object>> votes = voteCounts.entrySet().stream()
                    .map(entry -> {
                        Map<String, Object> map = new java.util.HashMap<>();
                        map.put("participantId", entry.getKey().getId());
                        map.put("participantName", entry.getKey().getName());
                        map.put("count", entry.getValue().intValue()); // Long을 int로 변환
                        return map;
                    })
                    // 투표 수가 많은 순서대로(내림차순) 정렬
                    .sorted((m1, m2) -> Integer.compare((Integer) m2.get("count"), (Integer) m1.get("count")))
                    .collect(Collectors.toList());

            return Map.of("votes", votes);
        }

        // 알 수 없는 유형인 경우 빈 객체 반환
        return Map.of();
    }
    @Transactional
    public RoomHostResponse getRoomHost(String roomCode) {
        // 1. 방 조회
        Room room = roomRepository.findByCode(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));

        // 2. 해당 방의 HOST 역할을 가진 참가자 조회
        Participant host = participantRepository.findByRoomAndRole(room, ParticipantRole.HOST)
                .orElseThrow(() -> new IllegalStateException("해당 방에 방장이 존재하지 않습니다."));

        // 3. 방장 이름 반환
        return new RoomHostResponse(host.getName());
    }

    @Transactional
    public RoomNameResponse getRoomName(String roomCode) {
        // 1. 방 조회
        Room room = roomRepository.findByCode(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));

        // 2. 방 이름 반환
        return new RoomNameResponse(room.getName());
    }
}
