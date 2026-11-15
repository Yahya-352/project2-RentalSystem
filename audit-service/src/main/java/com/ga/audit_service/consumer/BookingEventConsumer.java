package com.ga.audit_service.consumer;

import com.ga.audit_service.repository.AuditLogRepository;
import com.ga.audit_service.service.AuditLogService;
import com.ga.common.events.BookingEvent;
import com.ga.common.events.Topics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingEventConsumer {

    private final AuditLogService auditLogService;
    private final AuditLogRepository auditLogRepository;

    @KafkaListener(topics = Topics.BOOKING)
    public void consume(BookingEvent event) {
        if (event.eventId() != null && auditLogRepository.existsByEventId(event.eventId())) {
            return;
        }

        auditLogService.log(event.actorId(), event.eventType(), "Booking",
                event.bookingId(), event.message(), event.occurredAt(), event.eventId());
    }
}
