package com.evbooking.server.booking.controller;

import com.evbooking.server.booking.dto.CreateBookingRequest;
import com.evbooking.server.booking.dto.BookingResponse;
import com.evbooking.server.booking.dto.UpdateBookingRequest;
import com.evbooking.server.booking.service.BookingService;
import jakarta.validation.Valid;
<<<<<<< HEAD
import org.springframework.security.access.prepost.PreAuthorize;
=======
>>>>>>> main
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @PreAuthorize("hasRole('DRIVER') or hasRole('ADMIN')")
    public BookingResponse createBooking(
<<<<<<< HEAD
            Authentication authentication,
            @Valid @RequestBody
            CreateBookingRequest request
    ) {

        return bookingService.createBooking(
                authentication.getName(),
                request
        );
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('DRIVER') or hasRole('ADMIN')")
    public List<BookingResponse> getMyBookings(Authentication authentication) {

        return bookingService.getMyBookings(
                authentication.getName()
        );
=======
            @Valid @RequestBody CreateBookingRequest request,
            Authentication auth
    ) {
        Long userId = (Long) auth.getPrincipal();
        return bookingService.createBooking(request, userId);
    }

    @GetMapping("/my")
    public List<BookingResponse> getMyBookings(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return bookingService.getMyBookings(userId);
>>>>>>> main
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<BookingResponse> getAllBookings() {
        return bookingService.getAllBookings();
    }

    @DeleteMapping("/{id}")
<<<<<<< HEAD
    @PreAuthorize("hasRole('DRIVER') or hasRole('ADMIN')")
    public BookingResponse cancelBooking(
            Authentication authentication,
            @PathVariable Long id
    ) {
        return bookingService.cancelBooking(
                authentication.getName(),
                isAdmin(authentication),
                id
        );
=======
    public BookingResponse cancelBooking(@PathVariable Long id) {
        return bookingService.cancelBooking(id);
>>>>>>> main
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DRIVER') or hasRole('ADMIN')")
    public BookingResponse updateBooking(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody UpdateBookingRequest request
    ) {
<<<<<<< HEAD
        return bookingService.updateBooking(
                authentication.getName(),
                isAdmin(authentication),
                id,
                request
        );
=======
        return bookingService.updateBooking(id, request);
>>>>>>> main
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities()
                .stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
