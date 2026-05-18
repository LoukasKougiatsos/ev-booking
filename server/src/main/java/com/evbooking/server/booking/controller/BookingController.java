package com.evbooking.server.booking.controller;

import com.evbooking.server.booking.dto.CreateBookingRequest;
import com.evbooking.server.booking.service.BookingService;
import com.evbooking.server.entity.Booking;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

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
    public Booking createBooking(
            @Valid @RequestBody
            CreateBookingRequest request
    ) {

        return bookingService.createBooking(
                request
        );
    }
}