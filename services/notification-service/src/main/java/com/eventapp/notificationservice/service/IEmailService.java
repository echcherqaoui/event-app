package com.eventapp.notificationservice.service;

import com.eventapp.contracts.booking.v1.BookingConfirmed;

public interface IEmailService {
    void sendBookingEmail(BookingConfirmed event, String messageId);
}
