package com.eventapp.notificationservice.service.impl;

import com.eventapp.contracts.booking.v1.BookingConfirmed;
import com.eventapp.notificationservice.exception.domain.NotificationException;
import com.eventapp.notificationservice.repository.NotificationRepository;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.function.Consumer;

import static com.eventapp.notificationservice.enums.NotificationStatus.SENT;
import static com.eventapp.notificationservice.exception.enums.ErrorCode.NOTIFICATION_TEMPLATE_ERROR;
import static com.eventapp.notificationservice.exception.enums.ErrorCode.SMTP_CONNECTION_FAILED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Email Service Implementation Unit Tests")
class EmailServiceImplTest {

    @Mock private JavaMailSender mailSender;
    @Mock private TemplateEngine templateEngine;
    @Mock private NotificationRepository notificationRepository;
    @Mock private TransactionTemplate transactionTemplate;
    @Mock private TransactionStatus transactionStatus;
    @Mock private MimeMessage mimeMessage;

    private EmailServiceImpl emailService;
    private BookingConfirmed event;
    private static final String MESSAGE_ID = "msg-123";
    private static final String MAIL_FROM = "no-reply@eventapp.com";

    @BeforeEach
    void setUp() {
        emailService = new EmailServiceImpl(
              mailSender, templateEngine, notificationRepository, transactionTemplate, MAIL_FROM);

        event = BookingConfirmed.newBuilder()
              .setBookingId("BOOK-99")
              .setTotalAmount(15050L)
              .setUserEmail("user@example.com")
              .setEventId("EVT-1")
              .build();

        // Standard TransactionTemplate Mock
        lenient().doAnswer(invocation -> {
            Consumer<TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(transactionStatus);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        // Standard MimeMessage Mock
        lenient().when(mailSender.createMimeMessage())
              .thenReturn(mimeMessage);
    }

    @Nested
    @DisplayName("Idempotency & Initial Persistence")
    class PersistenceTests {

        @Test
        @DisplayName("Should skip processing if notification already exists (Duplicate)")
        void shouldSkipProcessing_WhenNotificationAlreadyExists() {
            doThrow(DataIntegrityViolationException.class)
                  .when(notificationRepository).save(any());

            emailService.sendBookingEmail(event, MESSAGE_ID);

            verify(notificationRepository).save(any());
            verify(templateEngine, never()).process(anyString(), any(Context.class));
            verify(mailSender, never()).send(any(MimeMessage.class));
        }
    }

    @Nested
    @DisplayName("Rendering & Delivery")
    class DeliveryTests {

        @Test
        @DisplayName("Should successfully render, send, and update status")
        void shouldCompleteFlow_WhenRenderingAndDeliverySucceed() {
            String renderedHtml = "<html>Confirmed</html>";
            when(templateEngine.process(eq("emails/booking-confirmed"), any(Context.class)))
                  .thenReturn(renderedHtml);

            emailService.sendBookingEmail(event, MESSAGE_ID);

            // Verify Rendering Data
            ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
            verify(templateEngine).process(anyString(), contextCaptor.capture());
            assertThat(contextCaptor.getValue().getVariable("bookingId")).isEqualTo("BOOK-99");
            assertThat(contextCaptor.getValue().getVariable("currency")).isEqualTo("MAD");

            // Verify SMTP Call
            verify(mailSender).send(mimeMessage);

            // Verify Final State Transition
            verify(notificationRepository).updateStatusAndSentAtByMessageId(SENT, MESSAGE_ID);
        }

        @Test
        @DisplayName("Should throw NotificationException on SMTP failure")
        void shouldThrowNotificationException_WhenSmtpFails() {
            when(templateEngine.process(anyString(), any(Context.class)))
                  .thenReturn("html");

            doThrow(new MailSendException("SMTP Down"))
                  .when(mailSender)
                  .send(any(MimeMessage.class));

            assertThatThrownBy(() -> emailService.sendBookingEmail(event, MESSAGE_ID))
                  .isInstanceOf(NotificationException.class)
                  .extracting("errorCode") // Accesses getErrorCode()
                  .isEqualTo(SMTP_CONNECTION_FAILED);

            verify(notificationRepository, never())
                  .updateStatusAndSentAtByMessageId(any(), any());
        }

        @Test
        @DisplayName("Should throw NotificationException if template engine fails")
        void shouldThrowNotificationException_WhenTemplateEngineFails() {
            when(templateEngine.process(anyString(), any(Context.class)))
                  .thenThrow(new RuntimeException("Template not found"));

            assertThatThrownBy(() -> emailService.sendBookingEmail(event, MESSAGE_ID))
                  .isInstanceOf(NotificationException.class)
                  .extracting("errorCode")
                  .isEqualTo(NOTIFICATION_TEMPLATE_ERROR);
        }
    }
}