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
import com.evbooking.server.entity.User;
import java.util.List;
import com.evbooking.server.booking.dto.UpdateBookingRequest;
import com.evbooking.server.booking.exception.ForbiddenOperationException;
import java.time.OffsetDateTime;
import com.evbooking.server.booking.exception.NotFoundException;

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
                        new NotFoundException("Booking not found"));



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

        User user = new User();
        user.setId(1L);

        booking.setUser(user);

        booking.setConnector(connector);
        booking.setStartTime(request.startTime());
        booking.setEndTime(request.endTime());
        booking.setStatus(BookingStatus.ACTIVE);

        return bookingRepository.save(booking);
    }

    public List<Booking> getMyBookings(
            Long userId
    ) {

        return bookingRepository.findByUserId(
                userId
        );
    }

    @Transactional
    public Booking cancelBooking(Long bookingId) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));

        if (booking.getStartTime().isBefore(OffsetDateTime.now())) {
            throw new ForbiddenOperationException(
                    "Cannot cancel a booking that has already started"
            );
        }
        booking.setStatus(BookingStatus.CANCELLED);

        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking updateBooking(
            Long bookingId,
            UpdateBookingRequest request
    ) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));

        if (booking.getStartTime().isBefore(OffsetDateTime.now())) {
            throw new ForbiddenOperationException(
                    "Cannot modify a booking that has already started"
            );
        }

        if (!request.endTime().isAfter(request.startTime())) {
            throw new ConflictException(
                    "End time must be after start time"
            );
        }

        if (booking.getStartTime().isBefore(OffsetDateTime.now())) {
            throw new ForbiddenOperationException(
                    "Cannot modify a booking that has already started"
            );
        }

        boolean connectorConflict =
                bookingRepository.existsConflict(
                        booking.getConnector().getId(),
                        request.startTime(),
                        request.endTime()
                );

        if (connectorConflict) {
            throw new ConflictException(
                    "Booking slot already taken"
            );
        }

        boolean userOverlap =
                bookingRepository.existsUserOverlap(
                        booking.getUser().getId(),
                        request.startTime(),
                        request.endTime()
                );

        if (userOverlap) {
            throw new ConflictException(
                    "User already has an overlapping booking"
            );
        }

        booking.setStartTime(request.startTime());
        booking.setEndTime(request.endTime());

        return bookingRepository.save(booking);
    }
}