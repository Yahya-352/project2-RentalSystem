package com.ga.RentalSystem.dto.request;

public record ChangePasswordRequest(String oldPassword,
                                    String newPassword) {
}
