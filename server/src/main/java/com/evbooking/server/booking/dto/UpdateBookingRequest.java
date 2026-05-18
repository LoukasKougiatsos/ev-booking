package com.evbooking.server.booking.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record UpdateBookingRequest(

        @NotNull
        @Future
        OffsetDateTime startTime,

        @NotNull
        @Future
        OffsetDateTime endTime
) {
}