package com.evbooking.server.booking.ui.dto;

public record SlotAvailabilityDto(
        String startTime,
        String endTime,
        boolean available
) {
}
