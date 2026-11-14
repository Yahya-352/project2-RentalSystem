package com.ga.booking_service.client;

import com.ga.common.events.NotificationEvent;
import com.ga.common.events.Topics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;


@RequiredArgsConstructor
@Component
@Slf4j
public class NotificationClient {

    private final KafkaTemplate<String , Object> kafkaTemplate;

    public void sendEvent(Long userId, String eventName , Object data){
        try{
            kafkaTemplate.send(Topics.NOTIFICATION , String.valueOf(userId)
                    , new NotificationEvent(userId, eventName, data))
                    .whenComplete((result , ex) -> {
                if (ex != null){
                    log.error("Failed to send notification {}" , eventName , ex);
                }
            });
        }catch (Exception ex){
            log.error(ex.getMessage(), ex);
        }
    }
}
