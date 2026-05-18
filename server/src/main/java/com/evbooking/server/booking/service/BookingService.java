package com.evbooking.server.booking.service;

import com.evbooking.server.booking.dto.CreateBookingRequest;
import com.evbooking.server.booking.exception.ConflictException;
import com.evbooking.server.entity.Booking;
import com.evbooking.server.entity.BookingStatus;
import com.evbooking.server.entity.Connector;
import com.evbooking.server.repository.BookingRepository;
import com.evbooking.server.repository.ConnectorRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ConnectorRepository connectorRepository;

    public BookingService(
            BookingRepository bookingRepository,
            ConnectorRepository connectorRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.connectorRepository = connectorRepository;
    }

    @Transactional
    public Booking createBooking(
            CreateBookingRequest request
    ) {

        Connector connector =
                connectorRepository.findById(
                        request.connectorId()
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Connector not found"
                        )
                );

        if (!request.endTime().isAfter(request.startTime())) {
            throw new ConflictException(
                    "End time must be after start time"
            );
        }

        boolean conflict =
                bookingRepository.existsConflict(
                        connector.getId(),
                        request.startTime(),
                        request.endTime()
                );

        if (conflict) {
            throw new ConflictException(
                    "Booking slot already taken"
            );
        }

        Booking booking = new Booking();

        booking.setConnector(connector);
        booking.setStartTime(request.startTime());
        booking.setEndTime(request.endTime());
        booking.setStatus(BookingStatus.ACTIVE);

        return bookingRepository.save(booking);
    }
}