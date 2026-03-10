package com.eventapp.bookingservice.mapper;


import com.eventapp.bookingservice.dto.request.BookingRequestDTO;
import com.eventapp.bookingservice.dto.response.BookingResponseDTO;
import com.eventapp.bookingservice.model.Booking;
import com.eventapp.lib.booking.v1.BookingResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class BookingMapper {
    public Booking toEntity(BookingRequestDTO request,
                            UUID id,
                            String userId,
                            String userEmail,
                            BigDecimal price) {

        return new Booking()
              .setId(id)
              .setEventId(request.eventId())
              .setUserId(userId)
              .setUserEmail(userEmail)
              .setPrice(price)
              .setQuantity(request.quantity());
    }

    public BookingResponseDTO toResponseDTO(Booking booking, long ttl) {
        return new BookingResponseDTO(
              booking.getId(),
              booking.getStatus().name(),
              booking.getCreatedAt().plusSeconds(ttl)
        );
    }

    public BookingResponse toGrpcResponse(Booking booking) {
        BigDecimal total = booking.getPrice()
              .multiply(BigDecimal.valueOf(booking.getQuantity()));

        return BookingResponse.newBuilder()
              .setBookingId(booking.getId().toString())
              .setUserId(booking.getUserId())
              .setEmail(booking.getUserEmail())
              .setPrice(booking.getPrice().toString())
              .setQuantity(booking.getQuantity())
              .setEventId(booking.getEventId())
              .setTotalPrice(total.toString())
              .build();
    }
}