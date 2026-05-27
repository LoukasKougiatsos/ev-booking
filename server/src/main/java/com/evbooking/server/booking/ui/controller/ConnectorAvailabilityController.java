package com.evbooking.server.booking.ui.controller;

import com.evbooking.server.booking.ui.dto.ConnectorOptionDto;
import com.evbooking.server.booking.ui.dto.SlotAvailabilityDto;
import com.evbooking.server.booking.ui.service.ConnectorAvailabilityService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/connectors")
public class ConnectorAvailabilityController {

    private final ConnectorAvailabilityService connectorAvailabilityService;

    public ConnectorAvailabilityController(ConnectorAvailabilityService connectorAvailabilityService) {
        this.connectorAvailabilityService = connectorAvailabilityService;
    }

    @GetMapping
    public List<ConnectorOptionDto> listConnectors() {
        return connectorAvailabilityService.listConnectors();
    }

    @GetMapping("/{id}/slots")
    public List<SlotAvailabilityDto> listSlots(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return connectorAvailabilityService.listSlots(id, date);
    }
}
