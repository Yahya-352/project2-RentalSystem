package com.ga.booking_service.client;

import com.ga.common.exceptions.BadRequestException;
import com.ga.common.exceptions.InformationNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@Slf4j
public class CarClient {
    private final RestClient restClient;

    public CarClient(@Value("${car-service.url}") String baseUrl) {
        restClient = RestClient.create(baseUrl);
    }

    public CarSummary getCar(Long carId , String authHeader){
        try {
            return restClient.get().uri("/cars/{id}" , carId)
                    .header("Authorization" , authHeader)
                    .retrieve().body(CarSummary.class);
        }catch (HttpClientErrorException.NotFound e) {
            throw new InformationNotFoundException("Car Not Found");
        }catch (RestClientException ex){
            log.error("car-service unavailable: {}", ex.getMessage());
            throw new BadRequestException("Car service is unavailable, try again later");
        }
    }



}
