package com.eventapp.bookingservice.mapper;

import com.eventapp.bookingservice.model.Booking;
import com.eventapp.contracts.booking.v1.BookingConfirmed;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class BookingOutboxMapper {

    private Timestamp mapTimestamp(Instant instant) {
        return Timestamps.fromMillis(instant.toEpochMilli());
    }

    public BookingConfirmed toConfirmedProto(Booking booking,
                                             long totalAmount,
                                             Instant occurredAt){
        return BookingConfirmed.newBuilder()
              .setBookingId(booking.getId().toString())
              .setEventId(booking.getEventId().toString())
              .setUserId(booking.getUserId())
              .setUserEmail(booking.getUserEmail())
              .setTotalAmount(totalAmount)
              .setConfirmedAt(mapTimestamp(occurredAt))
              .build();

    }
}