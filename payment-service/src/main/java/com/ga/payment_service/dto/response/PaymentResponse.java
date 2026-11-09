package com.ga.payment_service.dto.response;


public record PaymentResponse(String status, String message,
                              String sessionId, String sessionUrl) {

}
