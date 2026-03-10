package com.eventapp.bookingservice.service;

import com.eventapp.bookingservice.dto.request.BookingRequestDTO;
import com.eventapp.bookingservice.dto.response.BookingResponseDTO;
import com.eventapp.bookingservice.model.Booking;

import java.util.UUID;

public interface IBookingService {
    Booking getPendingBookingById(UUID id);

    BookingResponseDTO reserveTickets(BookingRequestDTO request);

    void cancelReservation(Long eventId, UUID bookingId);

    void confirmPurchase(String userId,
                         UUID bookingId,
                         Long amountCent);

    void processCompensation(String paymentId,
                             String bookingId,
                             String reason);

    void handleAutomaticExpiration(UUID bookingId);

    void handleFailedPayment(UUID bookingId);
}
