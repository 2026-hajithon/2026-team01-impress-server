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
import com.impress.server.participant.domain.ParticipantRole;
import com.impress.server.room.domain.RoomStatus;
import com.impress.server.websocket.dto.response.ParticipantKickedResponse;

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

    @Transactional
    public void kick(
            String roomCode,
            Long requesterParticipantId,
            Long targetParticipantId
    ) {
        Room room = roomRepository.findByCode(roomCode)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 방입니다."
                        )
                );

        if (room.getStatus() != RoomStatus.WAITING) {
            throw new IllegalStateException(
                    "대기방에서만 참가자를 강퇴할 수 있습니다."
            );
        }

        Participant requester =
                participantRepository.findByIdAndRoom(
                        requesterParticipantId,
                        room
                ).orElseThrow(() ->
                        new IllegalArgumentException(
                                "해당 방의 참가자가 아닙니다."
                        )
                );

        if (requester.getRole() != ParticipantRole.HOST) {
            throw new IllegalStateException(
                    "방장만 참가자를 강퇴할 수 있습니다."
            );
        }

        if (requesterParticipantId.equals(targetParticipantId)) {
            throw new IllegalArgumentException(
                    "방장은 자신을 강퇴할 수 없습니다."
            );
        }

        Participant target =
                participantRepository.findByIdAndRoom(
                        targetParticipantId,
                        room
                ).orElseThrow(() ->
                        new IllegalArgumentException(
                                "강퇴 대상이 해당 방에 없습니다."
                        )
                );

        if (target.getRole() == ParticipantRole.HOST) {
            throw new IllegalArgumentException(
                    "방장은 강퇴할 수 없습니다."
            );
        }

        eventPublisher.broadcastToRoom(
                roomCode,
                WebSocketEventType.PARTICIPANT_KICKED,
                new ParticipantKickedResponse(targetParticipantId)
        );

        participantRepository.delete(target);
        participantRepository.flush();

        broadcastParticipantList(room);
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