package com.ga.notification_service.consumer;

import com.ga.common.events.NotificationEvent;
import com.ga.common.events.Topics;
import com.ga.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = Topics.NOTIFICATION)
    public void consume(NotificationEvent event) {
        notificationService.sendEvent(event.userId(), event.eventName(), event.data());
    }

}
