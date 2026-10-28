package com.ga.RentalSystem.scheduler;

import com.ga.RentalSystem.enums.BookingStatus;

import com.ga.RentalSystem.repository.BookingRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@Slf4j
@RequiredArgsConstructor
@Transactional
public class BookingScheduler {
    private final BookingRepository bookingRepository;

    @Scheduled(cron = "0 0 12 * * ?")
    public void completeBooking() {
        int count = bookingRepository.completeFinishedBookings
                (BookingStatus.APPROVED , BookingStatus.COMPLETED  , LocalDate.now());
        log.info("{} bookings marked COMPLETED", count);
    }

    @Scheduled(cron = "0 0 12 * * ?")
    public void rejectBooking() {
        int count = bookingRepository.rejectUnApprovedBookings
                (BookingStatus.PENDING , BookingStatus.REJECTED , LocalDate.now() );
        log.info("{} bookings marked Rejected", count);
    }


}
