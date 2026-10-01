package com.ga.RentalSystem.dto.request;

public record ResetPasswordToken(
        String token,
        String password) {
}
