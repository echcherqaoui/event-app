package com.eventapp.bookingservice.repository;

import com.eventapp.bookingservice.model.OutboxInventoryRelease;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxInventoryReleaseRepository extends JpaRepository<OutboxInventoryRelease, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
          @QueryHint(name = "jakarta.persistence.lock.timeout", value = "0")
    })
    @Query("FROM OutboxInventoryRelease o " +
                "WHERE o.processed = false AND o.retryCount < :maxRetries " +
                "ORDER BY o.createdAt ASC")
    List<OutboxInventoryRelease> findPendingRetries(@Param("maxRetries") int maxRetries, Pageable pageable);
}