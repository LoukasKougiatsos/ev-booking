package com.evbooking.server.booking.controller;

import com.evbooking.server.booking.dto.BookingResponse;
import com.evbooking.server.booking.dto.CreateBookingRequest;
import com.evbooking.server.booking.dto.UpdateBookingRequest;
import com.evbooking.server.booking.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
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
            Authentication authentication,
            @Valid @RequestBody CreateBookingRequest request
    ) {
        return bookingService.createBooking(authentication.getName(), request);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('DRIVER') or hasRole('ADMIN')")
    public List<BookingResponse> getMyBookings(Authentication authentication) {
        return bookingService.getMyBookings(authentication.getName());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<BookingResponse> getAllBookings() {
        return bookingService.getAllBookings();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('DRIVER') or hasRole('ADMIN')")
    public BookingResponse cancelBooking(
            Authentication authentication,
            @PathVariable Long id
    ) {
        return bookingService.cancelBooking(authentication.getName(), isAdmin(authentication), id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DRIVER') or hasRole('ADMIN')")
    public BookingResponse updateBooking(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody UpdateBookingRequest request
    ) {
        return bookingService.updateBooking(authentication.getName(), isAdmin(authentication), id, request);
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
