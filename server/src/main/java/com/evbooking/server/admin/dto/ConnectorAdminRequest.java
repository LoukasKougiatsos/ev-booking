package com.evbooking.server.admin.dto;

import java.math.BigDecimal;

public record ConnectorAdminRequest(
        Long stationId,
        String connectorType,
        BigDecimal maxKw
) {
}
