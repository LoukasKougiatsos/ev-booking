package com.evbooking.server.booking.service;

import com.evbooking.server.booking.dto.CreateBookingRequest;
import com.evbooking.server.booking.exception.ConflictException;
import com.evbooking.server.entity.Booking;
import com.evbooking.server.entity.BookingStatus;
import com.evbooking.server.entity.Connector;
import com.evbooking.server.repository.BookingRepository;
import com.evbooking.server.repository.ConnectorRepository;
import com.evbooking.server.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.evbooking.server.entity.User;
import java.util.List;
import com.evbooking.server.booking.dto.UpdateBookingRequest;
import com.evbooking.server.booking.exception.ForbiddenOperationException;
import java.time.OffsetDateTime;
import com.evbooking.server.booking.exception.NotFoundException;
import com.evbooking.server.booking.dto.BookingResponse;


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
    public BookingResponse createBooking(
            String email,
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

        bookingRepository.lockActiveBookingsForConnector(
                connector.getId()
        );

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

        booking.setUser(findUser(email));

        booking.setConnector(connector);
        booking.setStartTime(request.startTime());
        booking.setEndTime(request.endTime());
        booking.setStatus(BookingStatus.ACTIVE);

        Booking savedBooking = bookingRepository.save(booking);

        return toResponse(savedBooking);
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
    public BookingResponse cancelBooking(
            String email,
            boolean admin,
            Long bookingId
    ) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));

        ensureOwnerOrAdmin(booking, email, admin);

        if (booking.getStartTime().isBefore(OffsetDateTime.now())) {
            throw new ForbiddenOperationException(
                    "Cannot cancel a booking that has already started"
            );
        }
        booking.setStatus(BookingStatus.CANCELLED);

        Booking savedBooking = bookingRepository.save(booking);

        return toResponse(savedBooking);
    }

    @Transactional
    public BookingResponse updateBooking(
            String email,
            boolean admin,
            Long bookingId,
            UpdateBookingRequest request
    ) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));

        ensureOwnerOrAdmin(booking, email, admin);

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
                bookingRepository.existsUserOverlapExcludingBooking(
                        booking.getUser().getId(),
                        booking.getId(),
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

        Booking savedBooking = bookingRepository.save(booking);

        return toResponse(savedBooking);
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
