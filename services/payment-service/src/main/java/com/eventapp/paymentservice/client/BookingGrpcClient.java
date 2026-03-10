package com.eventapp.paymentservice.client;

import com.eventapp.lib.booking.v1.BookingRequest;
import com.eventapp.lib.booking.v1.BookingResponse;
import com.eventapp.lib.booking.v1.BookingServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class BookingGrpcClient {
    @GrpcClient("booking-service")
    private BookingServiceGrpc.BookingServiceBlockingStub bookingStub;

    public BookingResponse verifyBooking(UUID bookingId) {
        BookingRequest request = BookingRequest.newBuilder()
              .setBookingId(bookingId.toString())
              .build();

        return bookingStub.verifyBooking(request);
    }
}
