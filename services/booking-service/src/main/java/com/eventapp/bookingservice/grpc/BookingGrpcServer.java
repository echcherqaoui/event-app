package com.eventapp.bookingservice.grpc;

import com.eventapp.bookingservice.mapper.BookingMapper;
import com.eventapp.bookingservice.model.Booking;
import com.eventapp.bookingservice.service.IBookingService;
import com.eventapp.lib.booking.v1.BookingRequest;
import com.eventapp.lib.booking.v1.BookingResponse;
import com.eventapp.lib.booking.v1.BookingServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
public class BookingGrpcServer extends BookingServiceGrpc.BookingServiceImplBase {
    
    private final IBookingService bookingService;
    private final BookingMapper bookingMapper;

    @Override
    @PreAuthorize("isAuthenticated()")
    public void verifyBooking(BookingRequest request,
                              StreamObserver<BookingResponse> responseObserver) {
        // Convert String ID to UUID
        UUID bookingId = UUID.fromString(request.getBookingId());

        Booking booking = bookingService.getPendingBookingById(bookingId);

        // Map Entity to gRPC Response
        BookingResponse response = bookingMapper.toGrpcResponse(booking);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}