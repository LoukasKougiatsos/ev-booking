package com.evbooking.server.booking.ui.dto;

import java.math.BigDecimal;

public record ConnectorOptionDto(
        Long id,
        Long stationId,
        String stationName,
        String connectorType,
        BigDecimal maxKw
) {
}
