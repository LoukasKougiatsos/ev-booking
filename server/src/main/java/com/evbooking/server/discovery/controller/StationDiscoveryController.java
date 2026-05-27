package com.evbooking.server.discovery.controller;

import com.evbooking.server.discovery.dto.StationDiscoveryItem;
import com.evbooking.server.discovery.service.StationDiscoveryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stations")
public class StationDiscoveryController {

    private final StationDiscoveryService stationDiscoveryService;

    public StationDiscoveryController(StationDiscoveryService stationDiscoveryService) {
        this.stationDiscoveryService = stationDiscoveryService;
    }

    @GetMapping
    public List<StationDiscoveryItem> searchStations(
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(defaultValue = "12") Integer radius,
            @RequestParam(required = false) Integer limit
    ) {
        return stationDiscoveryService.search(latitude, longitude, radius, limit);
    }
}
