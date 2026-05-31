package com.evbooking.server.booking.service;

import com.evbooking.server.booking.dto.BookingResponse;
import com.evbooking.server.booking.dto.CreateBookingRequest;
import com.evbooking.server.booking.dto.UpdateBookingRequest;
import com.evbooking.server.booking.exception.ConflictException;
import com.evbooking.server.booking.exception.ForbiddenOperationException;
import com.evbooking.server.booking.exception.NotFoundException;
import com.evbooking.server.entity.Booking;
import com.evbooking.server.entity.BookingStatus;
import com.evbooking.server.entity.Connector;
import com.evbooking.server.entity.User;
import com.evbooking.server.repository.BookingRepository;
import com.evbooking.server.repository.ConnectorRepository;
import com.evbooking.server.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ConnectorRepository connectorRepository;
    private final UserRepository userRepository;

    public BookingService(
            BookingRepository bookingRepository,
            ConnectorRepository connectorRepository,
            UserRepository userRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.connectorRepository = connectorRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public BookingResponse createBooking(String email, CreateBookingRequest request) {

        Connector connector = connectorRepository.findById(request.connectorId())
                .orElseThrow(() -> new NotFoundException("Connector not found"));

        if (!request.endTime().isAfter(request.startTime())) {
            throw new ConflictException("End time must be after start time");
        }

        User user = findUser(email);

        boolean userOverlap = bookingRepository.existsUserOverlap(
                user.getId(),
                request.startTime(),
                request.endTime()
        );
        if (userOverlap) {
            throw new ConflictException("You already have an overlapping booking in that time period");
        }

        bookingRepository.lockActiveBookingsForConnector(connector.getId());

        boolean conflict = bookingRepository.existsConflict(
                connector.getId(),
                request.startTime(),
                request.endTime()
        );
        if (conflict) {
            throw new ConflictException("Booking slot already taken");
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setConnector(connector);
        booking.setStartTime(request.startTime());
        booking.setEndTime(request.endTime());
        booking.setStatus(BookingStatus.ACTIVE);

        return toResponse(bookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(String email) {
        User user = findUser(email);
        return bookingRepository.findByUserId(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getAllBookings() {
        return bookingRepository.findAllByOrderByStartTimeDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BookingResponse cancelBooking(String email, boolean admin, Long bookingId) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));

        ensureOwnerOrAdmin(booking, email, admin);

        if (booking.getStartTime().isBefore(OffsetDateTime.now())) {
            throw new ForbiddenOperationException("Cannot cancel a booking that has already started");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        return toResponse(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse updateBooking(String email, boolean admin, Long bookingId, UpdateBookingRequest request) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));

        ensureOwnerOrAdmin(booking, email, admin);

        if (booking.getStartTime().isBefore(OffsetDateTime.now())) {
            throw new ForbiddenOperationException("Cannot modify a booking that has already started");
        }

        if (!request.endTime().isAfter(request.startTime())) {
            throw new ConflictException("End time must be after start time");
        }

        boolean connectorConflict = bookingRepository.existsConflict(
                booking.getConnector().getId(),
                request.startTime(),
                request.endTime()
        );
        if (connectorConflict) {
            throw new ConflictException("Booking slot already taken");
        }

        boolean userOverlap = bookingRepository.existsUserOverlapExcludingBooking(
                booking.getUser().getId(),
                booking.getId(),
                request.startTime(),
                request.endTime()
        );
        if (userOverlap) {
            throw new ConflictException("User already has an overlapping booking");
        }

        booking.setStartTime(request.startTime());
        booking.setEndTime(request.endTime());

        return toResponse(bookingRepository.save(booking));
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ForbiddenOperationException("Authenticated user not found"));
    }

    private void ensureOwnerOrAdmin(Booking booking, String email, boolean admin) {
        if (!admin && !booking.getUser().getEmail().equals(email)) {
            throw new ForbiddenOperationException("Cannot access another user's booking");
        }
    }

    private BookingResponse toResponse(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getUser().getId(),
                booking.getConnector().getId(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getStatus().name(),
                booking.getCreatedAt(),
                booking.getUpdatedAt()
        );
    }
}
