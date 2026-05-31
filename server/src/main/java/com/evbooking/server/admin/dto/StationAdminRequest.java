package com.evbooking.server.admin.dto;

public record StationAdminRequest(
        String name,
        String address,
        Double latitude,
        Double longitude
) {
}
