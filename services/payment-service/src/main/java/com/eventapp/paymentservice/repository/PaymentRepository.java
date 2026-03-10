package com.eventapp.paymentservice.repository;

import com.eventapp.paymentservice.enums.PaymentStatus;
import com.eventapp.paymentservice.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findByStatusAndCreatedAtBefore(PaymentStatus status, LocalDateTime createdAt);

    @Modifying
    @Query("""
             UPDATE Payment p SET p.status = :newStatus, p.updatedAt = :now
                        WHERE p.id = :id AND p.status = :expected
           """)
    int updateStatusAtomic(@Param("id") UUID id,
                           @Param("now") LocalDateTime now,
                           @Param("expected") PaymentStatus expected,
                           @Param("newStatus") PaymentStatus newStatus);

    @Modifying
    @Query("""
             UPDATE Payment p SET p.status = :newStatus, p.updatedAt = :now
                        WHERE p.bookingId = :bookingId AND p.status = :expected
           """)
    int atomicRefund(@Param("bookingId") UUID bookingId,
                     @Param("now") LocalDateTime now,
                     @Param("expected") PaymentStatus expected,
                     @Param("newStatus") PaymentStatus newStatus);

}