package com.evbooking.server.booking.controller;

import com.evbooking.server.booking.dto.CreateBookingRequest;
import com.evbooking.server.booking.dto.BookingResponse;
import com.evbooking.server.booking.dto.UpdateBookingRequest;
import com.evbooking.server.booking.service.BookingService;
import jakarta.validation.Valid;
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
    public BookingResponse createBooking(
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
    }

    @DeleteMapping("/{id}")
    public BookingResponse cancelBooking(@PathVariable Long id) {
        return bookingService.cancelBooking(id);
    }

    @PutMapping("/{id}")
    public BookingResponse updateBooking(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBookingRequest request
    ) {
        return bookingService.updateBooking(id, request);
    }
}