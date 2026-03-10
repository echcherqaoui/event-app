package com.eventapp.bookingservice.grpc;

import com.eventapp.bookingservice.exceptions.domain.BookingNotFoundException;
import com.eventapp.bookingservice.mapper.BookingMapper;
import com.eventapp.bookingservice.model.Booking;
import com.eventapp.bookingservice.service.IBookingService;
import com.eventapp.lib.booking.v1.BookingRequest;
import com.eventapp.lib.booking.v1.BookingResponse;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static com.eventapp.bookingservice.exceptions.enums.ErrorCode.BOOKING_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Booking gRPC Server Unit Tests")
class BookingGrpcServerTest {

    @Mock private IBookingService bookingService;

    @Mock private StreamObserver<BookingResponse> responseObserver;

    @Mock private BookingMapper bookingMapper;

    @InjectMocks private BookingGrpcServer bookingGrpcServer;

    private static final UUID BOOKING_ID = UUID.randomUUID();

    @Test
    @DisplayName("Should successfully verify booking and return mapped response")
    void verifyBooking_ShouldReturnSuccessResponse_WhenBookingExists() {
        BookingRequest request = BookingRequest.newBuilder()
              .setBookingId(BOOKING_ID.toString())
              .build();

        Booking mockBooking = new Booking();

        BookingResponse mockResponse = BookingResponse.newBuilder()
              .setBookingId(BOOKING_ID.toString())
              .setTotalPrice("200.00")
              .build();

        when(bookingService.getPendingBookingById(BOOKING_ID))
              .thenReturn(mockBooking);

        when(bookingMapper.toGrpcResponse(mockBooking))
              .thenReturn(mockResponse);

        // Execute the gRPC method
        bookingGrpcServer.verifyBooking(request, responseObserver);

        //Capture the response sent to onNext
        ArgumentCaptor<BookingResponse> responseCaptor = ArgumentCaptor.forClass(BookingResponse.class);

        verify(responseObserver, times(1)).onNext(responseCaptor.capture());
        verify(responseObserver, times(1)).onCompleted();
        verify(responseObserver, never()).onError(any());

        InOrder inOrder = inOrder(responseObserver);
        inOrder.verify(responseObserver).onNext(any(BookingResponse.class));
        inOrder.verify(responseObserver).onCompleted();

        BookingResponse actualResponse = responseCaptor.getValue();
        assertThat(actualResponse.getBookingId()).isEqualTo(BOOKING_ID.toString());
        assertThat(actualResponse.getTotalPrice()).isEqualTo("200.00");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when UUID is malformed")
    void verifyBooking_InvalidUuid() {
        BookingRequest request = BookingRequest.newBuilder()
              .setBookingId("invalid-uuid-string")
              .build();

        assertThatThrownBy(() -> bookingGrpcServer.verifyBooking(request, responseObserver))
              .isInstanceOf(IllegalArgumentException.class);

        verify(responseObserver, never()).onNext(any());
    }

    @Test
    @DisplayName("Should propagate BookingNotFoundException when service cannot find the booking")
    void verifyBooking_ShouldThrowNotFound_WhenBookingDoesNotExist() {
        BookingRequest request = BookingRequest.newBuilder()
              .setBookingId(BOOKING_ID.toString())
              .build();

        // Mock must throw the exception
        when(bookingService.getPendingBookingById(BOOKING_ID))
              .thenThrow(new BookingNotFoundException(BOOKING_NOT_FOUND, BOOKING_ID));

        assertThatThrownBy(() -> bookingGrpcServer.verifyBooking(request, responseObserver))
              .isInstanceOf(BookingNotFoundException.class)
              .hasMessageContaining(BOOKING_ID.toString());

        // Verify the observer was never interacted with because the exception halted execution
        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
    }
}