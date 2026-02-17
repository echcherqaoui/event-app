package com.eventapp.bookingservice.repository;

import com.eventapp.bookingservice.enums.BookingStatus;
import com.eventapp.bookingservice.model.Booking;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    @Modifying
    @Query("""
            UPDATE Booking b SET b.status = 'EXPIRED'
                WHERE b.id = :id AND b.status = 'PENDING'
           """)
    int markAsExpired(@Param("id") UUID id);

    @Query("""
            SELECT b.id FROM Booking b
                LEFT JOIN OutboxInventoryRelease o ON b.id = o.bookingId
                    WHERE b.status = 'PENDING'
                    AND b.createdAt < :cutoff
                    AND o.id IS NULL
           """)
    List<UUID> findStuckGhostBookings(@Param("cutoff") LocalDateTime cutoff, Pageable pageable);

    @Modifying
    @Query("""
            UPDATE Booking b SET b.status = 'CANCELLED'
                WHERE b.id = :bookingId
                AND b.userId = :userId
                AND b.eventId = :eventId
                AND b.status = :bookingStatus
            """)
    int cancelActiveBooking(@Param("bookingId") UUID bookingId,
                            @Param("userId") String userId,
                            @Param("eventId") long eventId,
                            @Param("bookingStatus") BookingStatus bookingStatus);

    @Query("select b.quantity from Booking b where b.id = :id ")
    Integer findQuantity(@Param("id")  UUID id);

}