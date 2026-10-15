package com.campushire.dto;

import com.campushire.entity.Role;
import com.campushire.entity.User;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInMillis,
        Long userId,
        String fullName,
        String email,
        Role role
) {
    public static AuthResponse from(User user, String token, long expiresInMillis) {
        return new AuthResponse(
                token,
                "Bearer",
                expiresInMillis,
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole()
        );
    }
}
