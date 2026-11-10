package com.ga.payment_service.client;

import com.ga.common.exceptions.BadRequestException;
import com.ga.common.exceptions.InformationNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Slf4j
@Component
public class BookingClient {

    private final RestClient restClient;
    private final String apiKey;


    public BookingClient(@Value("${booking-service.url}") String baseUrl,
                         @Value("${internal.api-key}") String apiKey) {
        restClient = RestClient.create(baseUrl);
        this.apiKey = apiKey;
    }

    public BookingDTO getBooking(Long bookingId ,String authHeader){
        try {
            return restClient.get()
                    .uri("/bookings/{id}" , bookingId)
                    .header("Authorization",authHeader)
                    .retrieve()
                    .body(BookingDTO.class);
        }
        catch (HttpClientErrorException.NotFound e) {
            throw new InformationNotFoundException("Booking Not Found");
        }
        catch (RestClientException ex){
            log.error("booking-service unavailable: {}", ex.getMessage());
            throw new BadRequestException("Booking service is unavailable, try again later");
        }
    }

    public void markPaid(Long bookingId){
        restClient.patch().uri("/bookings/{id}/paid" , bookingId)
                .header("X-Internal-Key", apiKey)
                .retrieve().toBodilessEntity();
    }

    public void markRefunded(Long bookingId){
        restClient.patch().uri("/bookings/{id}/refund" , bookingId)
                .header("X-Internal-Key", apiKey)
                .retrieve().toBodilessEntity();
    }

}
