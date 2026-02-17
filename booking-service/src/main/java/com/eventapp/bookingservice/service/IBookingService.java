package com.eventapp.bookingservice.service;

import com.eventapp.bookingservice.dto.request.BookingRequestDTO;
import com.eventapp.bookingservice.dto.response.BookingResponseDTO;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface IBookingService {
    @Transactional
    BookingResponseDTO reserveTickets(BookingRequestDTO request);

    @Transactional
    void cancelReservation(Long eventId, UUID bookingId);

    void handleAutomaticExpiration(UUID bookingId);
}
