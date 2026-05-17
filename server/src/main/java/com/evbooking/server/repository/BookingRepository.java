package com.evbooking.server.repository;

import com.evbooking.server.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import java.time.OffsetDateTime;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT COUNT(b) > 0
        FROM Booking b
        WHERE b.connector.id = :connectorId
        AND b.status = 'ACTIVE'
        AND (
            :startTime < b.endTime
            AND :endTime > b.startTime
        )
    """)
    boolean existsConflict(
            Long connectorId,
            OffsetDateTime startTime,
            OffsetDateTime endTime
    );
}