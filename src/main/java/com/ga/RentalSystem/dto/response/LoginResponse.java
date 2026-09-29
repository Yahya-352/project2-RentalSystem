package com.ga.RentalSystem.dto.response;

public record LoginResponse(
        String token,
        String email,
        String role
) { }
