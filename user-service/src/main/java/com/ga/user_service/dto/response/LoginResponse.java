package com.ga.user_service.dto.response;

public record LoginResponse(
        String token,
        String email,
        String role
) { }
