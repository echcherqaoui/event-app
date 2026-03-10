package com.eventapp.bookingservice.repository;

import com.eventapp.bookingservice.enums.BookingStatus;
import com.eventapp.bookingservice.model.Booking;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    Optional<Booking> findByIdAndStatus(UUID id, BookingStatus status);

    @QueryHints({@QueryHint(name = "javax.persistence.lock.timeout", value = "3000")})
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id ")
    Optional<Booking> findByIdWithLock(@Param("id") UUID id);

    @Query("""
            SELECT b.id FROM Booking b
                LEFT JOIN OutboxInventoryRelease o ON b.id = o.bookingId
                    WHERE b.status = 'PENDING'
                    AND b.createdAt < :cutoff
                    AND o.id IS NULL
           """)
    List<UUID> findStuckGhostBookings(@Param("cutoff") LocalDateTime cutoff, Pageable pageable);


    @Query("SELECT b.quantity FROM Booking b WHERE b.id = :id ")
    Integer findQuantity(@Param("id")  UUID id);


    @Transactional
    @Modifying
    @Query("""
            UPDATE Booking b SET b.status = :newStatus, b.updatedAt = :now
                WHERE b.id = :id AND b.status = 'PENDING'
           """)
    int updateStatusToIfPending(@Param("id") UUID id,
                                @Param("newStatus") BookingStatus newStatus,
                                @Param("now") LocalDateTime now);

    @Transactional
    @Modifying
    @Query("""
             UPDATE Booking b SET b.status = 'FAILED', b.updatedAt = :now
                    WHERE b.id = :id AND b.status  IN ('PENDING', 'EXPIRED')
           """)
    int markAsFailed(@Param("id") UUID id,
                     @Param("now") LocalDateTime now);

    @Transactional
    @Modifying
    @Query("""
            UPDATE Booking b SET b.status = 'CANCELLED', b.updatedAt = :now
                WHERE b.id = :bookingId
                AND b.userId = :userId
                AND b.eventId = :eventId
                AND b.status = :bookingStatus
            """)
    int cancelActiveBooking(@Param("bookingId") UUID bookingId,
                            @Param("userId") String userId,
                            @Param("eventId") long eventId,
                            @Param("bookingStatus") BookingStatus bookingStatus,
                            @Param("now") LocalDateTime now);

}