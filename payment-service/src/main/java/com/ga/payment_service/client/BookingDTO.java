package com.ga.payment_service.client;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BookingDTO(Long id , Long renterId
        , String status , BigDecimal totalPrice) {
}

