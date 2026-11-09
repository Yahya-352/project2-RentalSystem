package com.ga.booking_service.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@Slf4j
public class NotificationClient {

    private final RestClient restClient;
    private final String apikey;

    public NotificationClient(@Value("${notification-service.url}") String baseUrl,
                              @Value("${internal.api-key}") String apiKey) {
        restClient = RestClient.create(baseUrl);
        this.apikey = apiKey;
    }

    public void sendEvent(Long userId, String eventName , Object data){
        try{
            restClient.post().uri("/notifications")
                    .header("X-Internal-Key", apikey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "userId", userId,
                            "eventName", eventName,
                            "data", data))
                    .retrieve()
                    .toBodilessEntity();
        }catch (Exception ex){
            log.error(ex.getMessage(), ex);
        }
    }
}
