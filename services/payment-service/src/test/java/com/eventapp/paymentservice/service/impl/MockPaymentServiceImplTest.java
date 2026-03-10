package com.eventapp.paymentservice.service.impl;

import com.eventapp.lib.booking.v1.BookingResponse;
import com.eventapp.paymentservice.client.BookingServiceGateway;
import com.eventapp.paymentservice.dto.request.PaymentRequestDTO;
import com.eventapp.paymentservice.exception.domain.PaymentAlreadyProcessedException;
import com.eventapp.paymentservice.exception.domain.PaymentDeclinedException;
import com.eventapp.paymentservice.outbox.PaymentEventFactory;
import com.eventapp.paymentservice.model.Payment;
import com.eventapp.paymentservice.repository.OutboxEventRepository;
import com.eventapp.paymentservice.repository.PaymentRepository;
import com.eventapp.paymentservice.service.IGatewayService;
import com.eventapp.sharedutils.exceptions.domain.BusinessException;
import com.eventapp.sharedutils.exceptions.domain.ForbiddenException;
import com.eventapp.sharedutils.security.ISecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.function.Consumer;

import static com.eventapp.paymentservice.enums.PaymentStatus.PENDING;
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
@DisplayName("Mock Payment Service Unit Tests")
class MockPaymentServiceImplTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private OutboxEventRepository outboxRepository;
    @Mock private IGatewayService gatewayService;
    @Mock private ISecurityService securityService;
    @Mock private BookingServiceGateway bookingServiceGateway;
    @Mock private PaymentEventFactory paymentEventFactory;
    @Mock private TransactionStatus transactionStatus;
    @Mock private TransactionTemplate transactionTemplate;

    @InjectMocks private MockPaymentServiceImpl paymentService;

    private static final String USER_ID = "user-123";
    private static final UUID BOOKING_ID = UUID.randomUUID();
    private PaymentRequestDTO requestDTO;
    private BookingResponse verifiedBooking;

    @BeforeEach
    void setUp() {
        requestDTO = new PaymentRequestDTO(BOOKING_ID, "pm_visa");
        verifiedBooking = BookingResponse.newBuilder()
              .setUserId(USER_ID)
              .setTotalPrice("100.00")
              .build();

        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(transactionStatus);
        });

        lenient().doAnswer(invocation -> {
            Consumer<TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(transactionStatus);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    @Nested
    @DisplayName("Validation & Security Phase")
    class SecurityTests {
        @Test
        @DisplayName("Should throw Forbidden when authenticated user doesn't match booking user")
        void processPayment_SecurityViolation() {
            when(securityService.getAuthenticatedUserId())
                  .thenReturn("different-user");

            when(bookingServiceGateway.getBookingVerification(BOOKING_ID))
                  .thenReturn(verifiedBooking);

            assertThatThrownBy(() -> paymentService.processPayment(requestDTO))
                  .isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("Persistence & Gateway Phase")
    class PersistenceTests {
        @Test
        @DisplayName("Should throw PaymentAlreadyProcessed if DB unique constraint fails")
        void processPayment_DuplicatePayment() {
            when(securityService.getAuthenticatedUserId())
                  .thenReturn(USER_ID);

            when(bookingServiceGateway.getBookingVerification(BOOKING_ID))
                  .thenReturn(verifiedBooking);

            when(paymentRepository.save(any()))
                  .thenThrow(DataIntegrityViolationException.class);

            assertThatThrownBy(() -> paymentService.processPayment(requestDTO))
                  .isInstanceOf(PaymentAlreadyProcessedException.class);
        }

        @Test
        @DisplayName("Should leave state PENDING and not write Outbox on technical Gateway error")
        void processPayment_TechnicalTimeout() {
            when(securityService.getAuthenticatedUserId())
                  .thenReturn(USER_ID);

            when(bookingServiceGateway.getBookingVerification(BOOKING_ID))
                  .thenReturn(verifiedBooking);

            when(paymentRepository.save(any()))
                  .thenReturn(new Payment().setId(UUID.randomUUID()));

            // Simulation of a Timeout/Generic exception
            doThrow(new RuntimeException("Gateway Down"))
                  .when(gatewayService)
                  .charge(anyString(), any());

            assertThatThrownBy(() -> paymentService.processPayment(requestDTO))
                  .isInstanceOf(BusinessException.class);

            // Status update and Outbox NEVER called for technical errors
            verify(paymentRepository, never()).updateStatusAtomic(any(), any(), any(), any());
            verify(outboxRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Finalization & Atomic Updates")
    class FinalizationTests {
        @Test
        @DisplayName("Should complete payment and save Outbox on success")
        void processPayment_FullSuccess() {
            Payment mockPayment = new Payment().setId(UUID.randomUUID()).setStatus(PENDING);

            when(securityService.getAuthenticatedUserId())
                  .thenReturn(USER_ID);

            when(bookingServiceGateway.getBookingVerification(BOOKING_ID))
                  .thenReturn(verifiedBooking);

            when(paymentRepository.save(any()))
                  .thenReturn(mockPayment);

            // Successful update
            when(paymentRepository.updateStatusAtomic(any(), any(), eq(PENDING), any()))
                  .thenReturn(1);

            paymentService.processPayment(requestDTO);

            verify(gatewayService).charge(anyString(), any());
            verify(paymentRepository).updateStatusAtomic(any(), any(), eq(PENDING), any());
            verify(outboxRepository).save(any());
        }

        @Test
        @DisplayName("Should mark FAILED and save Outbox on PaymentDeclinedException")
        void processPayment_Declined() {
            Payment mockPayment = new Payment().setId(UUID.randomUUID());

            when(securityService.getAuthenticatedUserId())
                  .thenReturn(USER_ID);

            when(bookingServiceGateway.getBookingVerification(BOOKING_ID))
                  .thenReturn(verifiedBooking);

            when(paymentRepository.save(any()))
                  .thenReturn(mockPayment);

            doThrow(new PaymentDeclinedException())
                  .when(gatewayService)
                  .charge(anyString(), any());

            when(paymentRepository.updateStatusAtomic(any(), any(), eq(PENDING), any()))
                  .thenReturn(1);

            assertThatThrownBy(() -> paymentService.processPayment(requestDTO))
                  .isInstanceOf(PaymentDeclinedException.class);

            // We finalize even on decline to inform other services via Kafka
            verify(paymentRepository).updateStatusAtomic(any(), any(), any(), any());
            verify(outboxRepository).save(any());
        }
    }
}