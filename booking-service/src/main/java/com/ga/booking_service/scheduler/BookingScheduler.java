package com.ga.booking_service.scheduler;

import com.ga.booking_service.client.AuditClient;
import com.ga.booking_service.enums.BookingStatus;
import com.ga.booking_service.repository.BookingRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Daily background jobs that move bookings forward as time passes. Each job is a single bulk
 * UPDATE, so a booking that is cancelled or approved at the same moment is re-checked by the
 * database and skipped instead of being overwritten. Runs on Spring's scheduler thread.
 */
@Component
@Slf4j
@RequiredArgsConstructor
@Transactional
public class BookingScheduler {
    private final BookingRepository bookingRepository;
    private final AuditClient auditClient;

    /**
     * Every day at 12:00: approved bookings whose return date has passed become COMPLETED.
     * Writes one audit entry per run when something changed.
     */
    @Scheduled(cron = "0 0 12 * * ?")
    public void completeBooking() {
        int count = bookingRepository.completeFinishedBookings
                (BookingStatus.APPROVED , BookingStatus.COMPLETED  , LocalDate.now());
        String message = count + " bookings completed (rental period ended)";
        log.info(message);
        if (count > 0) {
            auditClient.log(null, "BOOKINGS_COMPLETED", "Booking", null, message);
        }
    }

    /**
     * Every day at 12:00: pending requests the agency never answered, whose pick-up date has
     * passed, become REJECTED. Writes one audit entry per run when something changed.
     */
    @Scheduled(cron = "0 0 12 * * ?")
    public void rejectBooking() {
        int count = bookingRepository.rejectUnApprovedBookings
                (BookingStatus.PENDING , BookingStatus.REJECTED , LocalDate.now() );
        String message = count + " pending bookings rejected automatically (pick-up date passed)";
        log.info(message);
        if (count > 0) {
            auditClient.log(null, "BOOKINGS_AUTO_REJECTED", "Booking", null, message);
        }
    }


}
