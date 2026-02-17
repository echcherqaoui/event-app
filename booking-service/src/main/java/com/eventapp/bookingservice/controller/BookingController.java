package com.eventapp.bookingservice.controller;

import com.eventapp.bookingservice.dto.request.BookingRequestDTO;
import com.eventapp.bookingservice.dto.response.BookingResponseDTO;
import com.eventapp.bookingservice.service.IBookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("${api.base-path}/bookings")
@RequiredArgsConstructor
@Validated
public class BookingController {

    private final IBookingService bookingService;

    @PostMapping("/reserve")
    public ResponseEntity<BookingResponseDTO> reserve(
          @RequestBody @Valid BookingRequestDTO request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
              .body(bookingService.reserveTickets(request));
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<Void> cancelBooking(@PathVariable("bookingId") UUID bookingId,
                                              @RequestParam("eventId") Long eventId) {
        bookingService.cancelReservation(
              eventId,
              bookingId
        );

        return ResponseEntity.noContent().build();
    }
}