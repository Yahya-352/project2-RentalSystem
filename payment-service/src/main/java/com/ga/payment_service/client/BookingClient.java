package com.ga.payment_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;

public class BookingClient {
    RestClient restClient;

    public BookingClient(@Value("${booking-service.url}") String baseUrl) {
        restClient = RestClient.create(baseUrl);
    }

    public BookingDTO getBooking(Long bookingId){
        return restClient.get()
                .uri()
                .retrieve()
                .body(BookingDTO.class);
    }

}
