package com.ga.RentalSystem.dto.response;

public record RegisterResponse(
        Long id,
        String userName,
        String email,
        String status,
        String message
) { }
