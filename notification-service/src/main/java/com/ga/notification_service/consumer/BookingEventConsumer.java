package com.ga.notification_service.consumer;

import com.ga.common.events.BookingEvent;
import com.ga.common.events.Topics;
import com.ga.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = Topics.BOOKING)
    public void consume(BookingEvent event) {
        Long recipientId = switch (event.eventType()) {
            case "BOOKING_CREATED" -> event.ownerId();
            case "BOOKING_APPROVED", "BOOKING_REJECTED" -> event.renterId();
            case "BOOKING_CANCELLED" -> event.actorId().equals(event.renterId())
                    ? event.ownerId()
                    : event.renterId();
            default -> null;
        };

        if (recipientId == null) {
            return;
        }

        notificationService.sendEvent(recipientId, event.eventType(), event.booking());
    }
}