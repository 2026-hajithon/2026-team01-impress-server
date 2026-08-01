package com.impress.server.room.repository;

import com.impress.server.room.domain.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByCode(String code);

    boolean existsByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT room
        FROM Room room
        WHERE room.code = :code
        """)
    Optional<Room> findByCodeForUpdate(
            @Param("code") String code
    );
}