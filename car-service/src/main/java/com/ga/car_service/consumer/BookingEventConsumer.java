package com.ga.car_service.consumer;

import com.ga.car_service.model.CarReservation;
import com.ga.car_service.repository.CarReservationRepository;
import com.ga.common.events.BookingEvent;
import com.ga.common.events.Topics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class BookingEventConsumer {

    private final CarReservationRepository carReservationRepository;

    @KafkaListener(topics = Topics.BOOKING)
    public void consume(BookingEvent event) {
        switch (event.eventType()) {
            case "BOOKING_APPROVED" -> saveReservation(event);
            case "BOOKING_CANCELLED" -> carReservationRepository.deleteById(event.bookingId());
            default -> { }
        }
    }

    private void saveReservation(BookingEvent event) {
        if (!(event.booking() instanceof Map<?, ?> booking)) {
            return;
        }
        CarReservation reservation = new CarReservation();
        reservation.setBookingId(event.bookingId());
        reservation.setCarId(((Number) booking.get("carId")).longValue());
        reservation.setStartDate(LocalDate.parse(booking.get("startDate").toString()));
        reservation.setEndDate(LocalDate.parse(booking.get("endDate").toString()));
        carReservationRepository.save(reservation);
    }
}
