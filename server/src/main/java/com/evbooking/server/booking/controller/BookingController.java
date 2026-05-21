package com.evbooking.server.booking.controller;

import com.evbooking.server.booking.dto.CreateBookingRequest;
import com.evbooking.server.booking.service.BookingService;
import com.evbooking.server.entity.Booking;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.evbooking.server.entity.Booking;
import com.evbooking.server.booking.dto.UpdateBookingRequest;
import com.evbooking.server.booking.dto.BookingResponse;
import java.util.List;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(
            BookingService bookingService
    ) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public BookingResponse createBooking(
            @Valid @RequestBody
            CreateBookingRequest request
    ) {

        return bookingService.createBooking(
                request
        );
    }

    @GetMapping("/my")
    public List<BookingResponse> getMyBookings() {

        return bookingService.getMyBookings(
                1L
        );
    }

    @DeleteMapping("/{id}")
    public BookingResponse cancelBooking(
            @PathVariable Long id
    ) {
        return bookingService.cancelBooking(id);
    }

    @PutMapping("/{id}")
    public BookingResponse updateBooking(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBookingRequest request
    ) {
        return bookingService.updateBooking(
                id,
                request
        );
    }
}