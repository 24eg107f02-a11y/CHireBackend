package com.campushire.dto;

import com.campushire.entity.Role;
import com.campushire.entity.User;

import java.time.Instant;

public record UserAdminResponse(
        Long id,
        String fullName,
        String email,
        Role role,
        boolean enabled,
        Instant createdAt
) {
    public static UserAdminResponse from(User user) {
        return new UserAdminResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }
}
