package com.eventapp.notificationservice.service.impl;

import com.eventapp.contracts.booking.v1.BookingConfirmed;
import com.eventapp.notificationservice.exception.domain.NotificationException;
import com.eventapp.notificationservice.model.Notification;
import com.eventapp.notificationservice.repository.NotificationRepository;
import com.eventapp.notificationservice.service.IEmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import static com.eventapp.notificationservice.enums.NotificationStatus.SENT;
import static com.eventapp.notificationservice.exception.enums.ErrorCode.NOTIFICATION_TEMPLATE_ERROR;
import static com.eventapp.notificationservice.exception.enums.ErrorCode.SMTP_CONNECTION_FAILED;

@Service
@Slf4j
public class EmailServiceImpl implements IEmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final NotificationRepository notificationRepository;
    private final TransactionTemplate transactionTemplate;
    private final String mailFrom;

    public EmailServiceImpl(JavaMailSender mailSender,
                            TemplateEngine templateEngine,
                            NotificationRepository notificationRepository,
                            TransactionTemplate transactionTemplate,
                            @Value("${app.mail.from}") String mailFrom) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.notificationRepository = notificationRepository;
        this.transactionTemplate = transactionTemplate;
        this.mailFrom = mailFrom;
    }

    private String renderHtml(BookingConfirmed event) {
        Context context = new Context();
        context.setVariable("bookingId", event.getBookingId());
        context.setVariable("amount", event.getTotalAmount());
        context.setVariable("currency", "MAD");

        return templateEngine.process("emails/booking-confirmed", context);
    }

    private void sendMimeMessage(String to,
                                 String htmlContent,
                                 String subject) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        helper.setFrom(mailFrom);

        mailSender.send(message);
    }

    private boolean persistNotification(BookingConfirmed event,
                                        String messageId,
                                        String subject) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                Notification n = new Notification()
                      .setMessageId(messageId)
                      .setSubject(subject)
                      .setBookingId(event.getBookingId())
                      .setEventId(event.getEventId())
                      .setRecipient(event.getUserEmail());

                notificationRepository.save(n);
            });
            return false;
        } catch (DataIntegrityViolationException e) {
            return true; // duplicate
        }
    }

    @Override
    public void sendBookingEmail(BookingConfirmed event, String messageId) {
        String subject = "Booking Confirmation: " + event.getBookingId();

        boolean alreadyStarted = persistNotification(
              event,
              messageId,
              subject
        );

        if (alreadyStarted) {
            log.warn("Duplicate detected. Skipping MessageID: {}", messageId);
            return;
        }

        try {
            // Rendering & Sending
            String htmlContent = renderHtml(event);
            sendMimeMessage(
                  event.getUserEmail(),
                  htmlContent,
                  subject
            );

            // Persistence (Separated Transaction)
            transactionTemplate.executeWithoutResult(status ->
                  notificationRepository.updateStatusAndSentAtByMessageId(
                        SENT,
                        messageId
                  )
            );
        } catch (MailException e) {
            log.error("SMTP Connection Failure for message [{}]: {}", messageId, e.getMessage(), e);

            throw new NotificationException(SMTP_CONNECTION_FAILED, messageId);

        } catch (Exception e) {
            log.error("Fatal Notification Error for booking [{}]: {}", event.getBookingId(), e.getMessage(), e);

            throw new NotificationException(NOTIFICATION_TEMPLATE_ERROR, event.getBookingId());
        }
    }
}