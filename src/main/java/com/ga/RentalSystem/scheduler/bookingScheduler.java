package com.ga.RentalSystem.scheduler;

import com.ga.RentalSystem.enums.BookingStatus;
import com.ga.RentalSystem.model.Booking;
import com.ga.RentalSystem.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@Slf4j
@RequiredArgsConstructor
public class bookingScheduler {
    private final BookingRepository bookingRepository;

    @Scheduled(cron = "0 0 12 * * ?")
    public void completeBooking() {
        bookingRepository.completeFinishedBookings
                (BookingStatus.APPROVED , BookingStatus.COMPLETED  , LocalDate.now());
    }

    @Scheduled(cron = "0 0 12 * * ?")
    public void rejectBooking() {
        bookingRepository.rejectUnApprovedBookings
                (BookingStatus.PENDING , BookingStatus.REJECTED , LocalDate.now() );
    }
}
