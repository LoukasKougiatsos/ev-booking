package com.evbooking.server.booking.dto;

import java.time.OffsetDateTime;

public record BookingResponse(
        Long id,
        Long userId,
        Long connectorId,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
