package com.evbooking.server.auth;

public record LoginResponse(
        String token,
        String role,
        Long userId
) {
}
