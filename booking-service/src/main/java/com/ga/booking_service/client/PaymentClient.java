package com.ga.booking_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;

public class PaymentClient {
    private final RestClient restClient;
    private String apikey;

    public PaymentClient(@Value("${payment-service.url}") String baseUrl,
                       @Value("${internal.api-key}") String apiKey) {
        restClient = RestClient.create(baseUrl);
        this.apikey = apiKey;
    }



}
