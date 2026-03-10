package com.eventapp.notificationservice.repository;

import com.eventapp.notificationservice.enums.NotificationStatus;
import com.eventapp.notificationservice.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    @Transactional
    @Modifying
    @Query("""
             Update Notification n set n.status = :newStatus
                        WHERE n.messageId = :messageId
           """)
    void updateStatusAndSentAtByMessageId(@Param("newStatus") NotificationStatus status,
                                          @Param("messageId") String messageId);
}