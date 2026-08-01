package com.impress.server.participant.repository;

import com.impress.server.participant.domain.Participant;
import com.impress.server.room.domain.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParticipantRepository
        extends JpaRepository<Participant, Long> {

    Optional<Participant> findByIdAndRoom(
            Long participantId,
            Room room
    );

    List<Participant> findAllByRoomOrderByIdAsc(
            Room room
    );

    boolean existsByRoomAndName(
            Room room,
            String name
    );

    long countByRoom(Room room);
}