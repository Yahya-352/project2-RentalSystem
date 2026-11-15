package com.ga.booking_service.client;

import com.ga.booking_service.dto.response.BookingResponse;
import com.ga.common.events.BookingEvent;
import com.ga.common.events.Topics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class BookingEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(String eventType, Long actorId, BookingResponse booking, String message) {
        BookingEvent event = new BookingEvent(
                UUID.randomUUID().toString(),
                eventType,
                booking.id(),
                actorId,
                booking.renterId(),
                booking.ownerId(),
                message,
                booking,
                LocalDateTime.now());
        try {
            kafkaTemplate.send(Topics.BOOKING, String.valueOf(booking.id()), event)
                    .whenComplete((res, ex) -> {
                        if (ex != null) {
                            log.error("Failed to send booking event {}", eventType, ex);
                        }
                    });
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }
}
