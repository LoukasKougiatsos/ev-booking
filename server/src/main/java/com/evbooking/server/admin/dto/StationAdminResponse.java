package com.evbooking.server.admin.dto;

import java.time.OffsetDateTime;

public record StationAdminResponse(
        Long id,
        String name,
        String address,
        Double latitude,
        Double longitude,
        OffsetDateTime createdAt
) {
}
