package com.impress.server.websocket.service;

import com.impress.server.game.domain.GameRound;
import com.impress.server.game.domain.GameRoundStatus;
import com.impress.server.game.domain.GameSession;
import com.impress.server.game.domain.GameSessionStatus;
import com.impress.server.game.domain.NextRoundVote;
import com.impress.server.game.repository.GameRoundRepository;
import com.impress.server.game.repository.NextRoundVoteRepository;
import com.impress.server.participant.domain.ConnectionStatus;
import com.impress.server.participant.domain.Participant;
import com.impress.server.participant.repository.ParticipantRepository;
import com.impress.server.room.domain.Room;
import com.impress.server.room.domain.RoomStatus;
import com.impress.server.room.repository.RoomRepository;
import com.impress.server.websocket.dto.request.NextRoundRequest;
import com.impress.server.websocket.dto.response.NextRoundVoteResponse;
import com.impress.server.websocket.dto.response.RoundStartResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NextRoundWebSocketService {

    private final RoomRepository roomRepository;
    private final ParticipantRepository participantRepository;
    private final GameRoundRepository gameRoundRepository;
    private final NextRoundVoteRepository nextRoundVoteRepository;
    private final GameWebSocketService gameWebSocketService;

    @Transactional
    public NextRoundResult vote(
            String roomCode,
            Long participantId,
            NextRoundRequest request
    ) {
        validateRequest(request);

        Room room = roomRepository.findByCodeForUpdate(roomCode)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 방입니다."
                        )
                );

        if (room.getStatus() != RoomStatus.PLAYING) {
            throw new IllegalStateException(
                    "게임이 진행 중인 방이 아닙니다."
            );
        }

        Participant participant =
                participantRepository.findByIdAndRoom(
                        participantId,
                        room
                ).orElseThrow(() ->
                        new IllegalArgumentException(
                                "해당 방의 참가자가 아닙니다."
                        )
                );

        if (participant.getConnectionStatus()
                != ConnectionStatus.CONNECTED) {
            throw new IllegalStateException(
                    "연결된 참가자만 투표할 수 있습니다."
            );
        }

        GameRound gameRound =
                gameRoundRepository.findByIdForUpdate(
                        request.roundId()
                ).orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 라운드입니다."
                        )
                );

        validateCurrentRound(gameRound, room);

        if (nextRoundVoteRepository
                .existsByGameRoundAndParticipant(
                        gameRound,
                        participant
                )) {
            throw new IllegalStateException(
                    "이미 다음 라운드 진행에 동의했습니다."
            );
        }

        nextRoundVoteRepository.saveAndFlush(
                new NextRoundVote(
                        gameRound,
                        participant
                )
        );

        long connectedParticipantCount =
                participantRepository
                        .findAllByRoomOrderByIdAsc(room)
                        .stream()
                        .filter(roomParticipant ->
                                roomParticipant
                                        .getConnectionStatus()
                                        == ConnectionStatus.CONNECTED
                        )
                        .count();

        long votedCount =
                nextRoundVoteRepository
                        .countByGameRoundAndParticipant_ConnectionStatus(
                                gameRound,
                                ConnectionStatus.CONNECTED
                        );

        long requiredCount =
                connectedParticipantCount / 2 + 1;

        NextRoundVoteResponse voteResponse =
                new NextRoundVoteResponse(
                        gameRound.getId(),
                        votedCount,
                        requiredCount
                );

        if (votedCount < requiredCount) {
            return new NextRoundResult(
                    voteResponse,
                    null,
                    false
            );
        }

        gameRound.complete();

        GameSession gameSession =
                gameRound.getGameSession();

        if (gameRound.getRoundOrder()
                < gameSession.getTotalRounds()) {

            GameRound nextRound =
                    gameRoundRepository
                            .findByGameSessionAndRoundOrder(
                                    gameSession,
                                    gameRound.getRoundOrder() + 1
                            )
                            .orElseThrow(() ->
                                    new IllegalStateException(
                                            "다음 라운드를 찾을 수 없습니다."
                                    )
                            );

            RoundStartResponse roundStartResponse =
                    gameWebSocketService.startNextRound(
                            nextRound
                    );

            return new NextRoundResult(
                    voteResponse,
                    roundStartResponse,
                    false
            );
        }

        gameSession.finish();
        room.finishGame();

        return new NextRoundResult(
                voteResponse,
                null,
                true
        );
    }

    private void validateRequest(
            NextRoundRequest request
    ) {
        if (request == null
                || request.roundId() == null
                || request.roundId() <= 0) {
            throw new IllegalArgumentException(
                    "올바른 라운드 ID가 필요합니다."
            );
        }
    }

    private void validateCurrentRound(
            GameRound gameRound,
            Room room
    ) {
        if (!gameRound.getGameSession()
                .getRoom()
                .getId()
                .equals(room.getId())) {
            throw new IllegalArgumentException(
                    "해당 방의 라운드가 아닙니다."
            );
        }

        if (gameRound.getGameSession().getStatus()
                != GameSessionStatus.PLAYING) {
            throw new IllegalStateException(
                    "현재 진행 중인 게임 세션이 아닙니다."
            );
        }

        if (gameRound.getStatus()
                != GameRoundStatus.RESULT) {
            throw new IllegalStateException(
                    "결과 화면에서만 다음 라운드 투표가 가능합니다."
            );
        }
    }

    public record NextRoundResult(
            NextRoundVoteResponse voteResponse,
            RoundStartResponse nextRound,
            boolean gameEnded
    ) {
    }
}