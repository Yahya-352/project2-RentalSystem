package com.ga.user_service.dto.response;

import com.ga.user_service.enums.Role;
import com.ga.user_service.enums.UserStatus;
import com.ga.user_service.model.User;

import java.time.LocalDateTime;

public record UserSummaryResponse(
        Long id,
        String userName,
        String email,
        Role role,
        UserStatus status,
        boolean verified,
        LocalDateTime createdAt
) {
    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getUserName(),
                user.getEmail(),
                user.getRoleEnum(),
                user.getUserStatus(),
                user.isVerified(),
                user.getCreatedAt());
    }
}
