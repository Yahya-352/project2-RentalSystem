package com.ga.RentalSystem.dto.response;


public record PaymentResponse(String status, String message,
                              String sessionId, String sessionUrl) {

}
