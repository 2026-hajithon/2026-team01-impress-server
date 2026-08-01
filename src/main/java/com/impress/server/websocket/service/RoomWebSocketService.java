package com.impress.server.websocket.service;

import com.impress.server.participant.domain.ConnectionStatus;
import com.impress.server.participant.domain.Participant;
import com.impress.server.participant.repository.ParticipantRepository;
import com.impress.server.room.domain.Room;
import com.impress.server.room.repository.RoomRepository;
import com.impress.server.websocket.dto.WebSocketEventType;
import com.impress.server.websocket.dto.response.ParticipantSummaryResponse;
import com.impress.server.websocket.publisher.WebSocketEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoomWebSocketService {

    private final RoomRepository roomRepository;
    private final ParticipantRepository participantRepository;
    private final WebSocketEventPublisher eventPublisher;

    public RoomWebSocketService(
            RoomRepository roomRepository,
            ParticipantRepository participantRepository,
            WebSocketEventPublisher eventPublisher
    ) {
        this.roomRepository = roomRepository;
        this.participantRepository = participantRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void enter(String roomCode, Long participantId) {
        Room room = roomRepository.findByCode(roomCode)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 방입니다."
                        )
                );

        Participant participant =
                participantRepository.findByIdAndRoom(
                        participantId,
                        room
                ).orElseThrow(() ->
                        new IllegalArgumentException(
                                "해당 방의 참가자가 아닙니다."
                        )
                );

        participant.connect();

        broadcastParticipantList(room);
    }

    @Transactional
    public void disconnect(Long participantId) {
        Participant participant =
                participantRepository.findById(participantId)
                        .orElse(null);

        if (participant == null) {
            return;
        }

        if (participant.getConnectionStatus()
                == ConnectionStatus.DISCONNECTED) {
            return;
        }

        participant.disconnect();

        broadcastParticipantList(participant.getRoom());
    }

    private void broadcastParticipantList(Room room) {
        List<ParticipantSummaryResponse> participants =
                participantRepository
                        .findAllByRoomOrderByIdAsc(room)
                        .stream()
                        .map(this::toSummaryResponse)
                        .toList();

        eventPublisher.broadcastToRoom(
                room.getCode(),
                WebSocketEventType.PARTICIPANT_LIST_UPDATE,
                participants
        );
    }

    private ParticipantSummaryResponse toSummaryResponse(
            Participant participant
    ) {
        return new ParticipantSummaryResponse(
                participant.getId(),
                participant.getName(),
                participant.getRole().name(),
                participant.getConnectionStatus().name()
        );
    }
}