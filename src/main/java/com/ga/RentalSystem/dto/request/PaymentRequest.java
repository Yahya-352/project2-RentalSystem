package com.ga.RentalSystem.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public record PaymentRequest(Long amount,
                             String currency) {

}
