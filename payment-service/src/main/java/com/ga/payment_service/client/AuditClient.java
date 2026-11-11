package com.ga.payment_service.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@Slf4j
public class AuditClient {

    RestClient restClient;
    private String apikey;

    public AuditClient(@Value("${audit-service.url}") String baseUrl,
                       @Value("${internal.api-key}") String apiKey) {
        restClient = RestClient.create(baseUrl);
        this.apikey = apiKey;
    }

    public void log(Long userId, String action, String entityType, Long entityId, String details){
        try{
            restClient.post().uri("/audit-logs")
                    .header("X-Internal-Key", apikey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new AuditLogRequest(userId, action, entityType, entityId, details))
                    .retrieve()
                    .toBodilessEntity();
        }catch (Exception ex){
            log.error(ex.getMessage(), ex);
        }
    }
}
