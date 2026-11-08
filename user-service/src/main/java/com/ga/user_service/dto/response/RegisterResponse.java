package com.ga.user_service.dto.response;

public record RegisterResponse(
        Long id,
        String userName,
        String email,
        String status,
        String message
) { }
