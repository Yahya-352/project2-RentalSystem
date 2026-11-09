package com.ga.booking_service.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;

@Component
@Slf4j
public class CarClient {
    RestClient restClient;
    private String apikey;

    public CarClient(@Value("${car-service.url}") String baseUrl,
                       @Value("${internal.api-key}") String apiKey) {
        restClient = RestClient.create(baseUrl);
        this.apikey = apiKey;
    }

    public CarSummary getCars(){
        try {
            return restClient.get().uri("/cars")
                    .header("X-Internal-Key", apikey)
                    .retrieve().body(CarSummary.class);
        }catch (Exception ex){
            log.error(ex.getMessage(), ex);
        }
    }



}
