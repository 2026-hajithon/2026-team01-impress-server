package com.impress.server.websocket.scheduler;

import com.impress.server.game.domain.GameRound;
import com.impress.server.game.domain.GameRoundStatus;
import com.impress.server.game.repository.GameRoundRepository;
import com.impress.server.room.domain.Room;
import com.impress.server.websocket.dto.WebSocketEventType;
import com.impress.server.websocket.publisher.WebSocketEventPublisher;
import com.impress.server.websocket.service.RoundResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RoundTimeoutScheduler {

    private final GameRoundRepository gameRoundRepository;
    private final RoundResultService roundResultService;
    private final WebSocketEventPublisher eventPublisher;

    @Scheduled(fixedDelay = 500L)
    @Transactional
    public void processExpiredRounds() {
        List<GameRound> expiredRounds =
                gameRoundRepository.findAllExpiredForUpdate(
                        GameRoundStatus.ANSWERING,
                        LocalDateTime.now()
                );

        for (GameRound gameRound : expiredRounds) {
            Room room =
                    gameRound.getGameSession().getRoom();

            roundResultService.createOnTimeout(
                    gameRound,
                    room
            ).ifPresent(response ->
                    eventPublisher.broadcastToRoom(
                            room.getCode(),
                            WebSocketEventType.ROUND_RESULT,
                            response
                    )
            );
        }
    }
}