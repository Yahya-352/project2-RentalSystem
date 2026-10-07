package com.ga.RentalSystem.dto.request;

import jakarta.validation.constraints.NotBlank;

public record MakeRequest(
        @NotBlank(message = "Make name is required")
        String name
) {}