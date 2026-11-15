package com.ga.booking_service.client;

import com.ga.common.events.AuditEvent;
import com.ga.common.events.Topics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditClient {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void log(Long userId, String action, String entityType, Long entityId, String details){
        try {
            kafkaTemplate.send(Topics.AUDIT ,
                            new AuditEvent(userId, action, entityType, entityId, details ,
                                    LocalDateTime.now() , UUID.randomUUID().toString()))
                    .whenComplete((res, ex) -> {
                        if (ex != null) {
                            log.error("Failed to send event {}", action ,ex);
                        }
                    });
        }catch (Exception ex){
            log.error(ex.getMessage(), ex);
        }
    }
}

