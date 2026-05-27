package com.evbooking.server.booking.ui.service;

import com.evbooking.server.booking.ui.dto.ConnectorOptionDto;
import com.evbooking.server.booking.ui.dto.SlotAvailabilityDto;
import com.evbooking.server.entity.BookingStatus;
import com.evbooking.server.entity.Booking;
import com.evbooking.server.repository.BookingRepository;
import com.evbooking.server.repository.ConnectorRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ConnectorAvailabilityService {

    private final ConnectorRepository connectorRepository;
    private final BookingRepository bookingRepository;

    public ConnectorAvailabilityService(ConnectorRepository connectorRepository, BookingRepository bookingRepository) {
        this.connectorRepository = connectorRepository;
        this.bookingRepository = bookingRepository;
    }

    public List<ConnectorOptionDto> listConnectors() {
        try {
            return connectorRepository.findAllWithStation().stream()
                    .map(connector -> new ConnectorOptionDto(
                            connector.getId(),
                            connector.getStation().getName(),
                            connector.getConnectorType().name(),
                            connector.getMaxKw()
                    ))
                    .toList();
        } catch (Exception ex) {
            return fallbackConnectors();
        }
    }

    public List<SlotAvailabilityDto> listSlots(Long connectorId, LocalDate date) {
        OffsetDateTime dayStart = date.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime dayEnd = dayStart.plusDays(1);

        List<Booking> bookings;
        try {
            bookings = bookingRepository.findActiveForConnectorBetween(connectorId, dayStart, dayEnd);
        } catch (Exception ex) {
            return fallbackSlots();
        }

        List<SlotAvailabilityDto> slots = new ArrayList<>();
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

        OffsetDateTime current = dayStart.plusHours(8);
        OffsetDateTime close = dayStart.plusHours(20);

        while (current.isBefore(close)) {
            OffsetDateTime slotStart = current;
            OffsetDateTime slotEnd = current.plusMinutes(30);
            boolean available = bookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.ACTIVE)
                    .noneMatch(b -> slotStart.isBefore(b.getEndTime()) && slotEnd.isAfter(b.getStartTime()));
            slots.add(new SlotAvailabilityDto(
                    slotStart.format(timeFmt),
                    slotEnd.format(timeFmt),
                    available
            ));
            current = slotEnd;
        }

        return slots;
    }

    private List<ConnectorOptionDto> fallbackConnectors() {
        return List.of(
                new ConnectorOptionDto(1L, "Berlin-Mitte HyperHub", "CCS", BigDecimal.valueOf(350)),
                new ConnectorOptionDto(2L, "Berlin-Mitte HyperHub", "CCS", BigDecimal.valueOf(350)),
                new ConnectorOptionDto(3L, "Berlin-Mitte HyperHub", "CHADEMO", BigDecimal.valueOf(50)),
                new ConnectorOptionDto(4L, "Berlin-Mitte HyperHub", "TYPE2", BigDecimal.valueOf(22))
        );
    }

    private List<SlotAvailabilityDto> fallbackSlots() {
        return List.of(
                new SlotAvailabilityDto("08:00", "08:30", true),
                new SlotAvailabilityDto("08:30", "09:00", true),
                new SlotAvailabilityDto("09:00", "09:30", false),
                new SlotAvailabilityDto("09:30", "10:00", true),
                new SlotAvailabilityDto("10:00", "10:30", true),
                new SlotAvailabilityDto("10:30", "11:00", true),
                new SlotAvailabilityDto("11:00", "11:30", true),
                new SlotAvailabilityDto("11:30", "12:00", true),
                new SlotAvailabilityDto("12:00", "12:30", false),
                new SlotAvailabilityDto("12:30", "13:00", true),
                new SlotAvailabilityDto("13:00", "13:30", true),
                new SlotAvailabilityDto("13:30", "14:00", true)
        );
    }
}
