package com.ga.RentalSystem.controller;

import com.ga.RentalSystem.dto.request.BookingRequest;
import com.ga.RentalSystem.dto.response.BookingResponse;
import com.ga.RentalSystem.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;

    @PostMapping
    public BookingResponse createBooking(@RequestBody BookingRequest request,
                                                         Authentication authentication) {
        BookingResponse response = bookingService.createBooking(request, authentication);
        return response;
    }

    @GetMapping("/me")
    public List<BookingResponse> getMyBookings(Authentication authentication) {
        return bookingService.getMyBookings(authentication);
    }


    @GetMapping("/my-cars")
    public List<BookingResponse> getBookingForMyCars(Authentication authentication) {
        return bookingService.getBookingForMyCars(authentication);
    }

    @PatchMapping("/{id}/approve")
    public BookingResponse approveBooking(@PathVariable("id") Long id,
                                          Authentication authentication) {
        return bookingService.approveBooking(id, authentication);
    }

    @PatchMapping("/{id}/reject")
    public BookingResponse rejectBooking(@PathVariable("id") Long id,
                                         Authentication authentication) {
        return bookingService.rejectBooking(id, authentication);
    }

    @PatchMapping("/{id}/cancel")
    public BookingResponse cancelBooking(@PathVariable("id") Long id,
                                         Authentication authentication) {
        return bookingService.cancelBooking(id, authentication);
    }

    @GetMapping
    public List<BookingResponse> getAllBookings(Authentication authentication) {
        return bookingService.getAllBookings(authentication);
    }
}
