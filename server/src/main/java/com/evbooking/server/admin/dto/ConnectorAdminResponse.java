package com.evbooking.server.admin.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ConnectorAdminResponse(
        Long id,
        Long stationId,
        String stationName,
        String connectorType,
        BigDecimal maxKw,
        OffsetDateTime createdAt
) {
}
