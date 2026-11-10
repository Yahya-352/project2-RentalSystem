package com.ga.booking_service.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class PaymentClient {
    private final RestClient restClient;
    private String apikey;

    public PaymentClient(@Value("${payment-service.url}") String baseUrl,
                       @Value("${internal.api-key}") String apiKey) {
        restClient = RestClient.create(baseUrl);
        this.apikey = apiKey;
    }

    public void refund(Long bookindId){
        restClient.post().uri("/pay/refund/{bookingId}" , bookindId)
                .header("X-Internal-Key", apikey)
                .retrieve().toBodilessEntity();
    }



}
