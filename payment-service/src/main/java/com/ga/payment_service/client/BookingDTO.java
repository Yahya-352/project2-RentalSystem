package com.ga.payment_service.client;

import java.math.BigDecimal;

public record BookingDTO(Long id , Long renterId
        , String bookingStatus , String model ,
                         BigDecimal totalPrice) {
}
