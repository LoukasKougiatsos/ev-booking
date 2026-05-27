package com.evbooking.server.discovery.dto;

public record StationDiscoveryItem(
        String id,
        String name,
        String address,
        String city,
        String region,
        String country,
        Double latitude,
        Double longitude,
        String connector,
        Integer powerKw,
        Integer level,
        Integer available,
        Integer total,
        Boolean active
) {
}
