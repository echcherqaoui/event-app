package com.eventapp.bookingservice.schedular;

import com.eventapp.bookingservice.repository.BookingRepository;
import com.eventapp.bookingservice.service.IBookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GhostBookingReaper {
    private final BookingRepository bookingRepository;
    private final IBookingService bookingService;

    @Scheduled(fixedDelayString = "PT5M")
    public void cleanUpGhostBookings() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(15);
        Pageable pageable = PageRequest.of(0, 50);
        List<UUID> ghosts;

        do {
            ghosts = bookingRepository.findStuckGhostBookings(cutoff, pageable);
            ghosts.forEach(bookingService::handleAutomaticExpiration);
            pageable = pageable.next();
        } while (!ghosts.isEmpty());
    }
}