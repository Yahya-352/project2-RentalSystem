package com.ga.RentalSystem.controller;

import com.ga.RentalSystem.dto.request.BookingRequest;
import com.ga.RentalSystem.dto.response.BookingResponse;
import com.ga.RentalSystem.dto.response.PageResponse;
import com.ga.RentalSystem.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Bookings", description = "Create, approve, reject, cancel and view car bookings")
@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;

    @Operation(summary = "Create a booking for a car (customer only)")
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<BookingResponse> createBooking(@RequestBody @Valid BookingRequest request,
                                                         Authentication authentication) {
        BookingResponse response = bookingService.createBooking(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "View your own bookings (customer only)")
    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<BookingResponse> getMyBookings(Authentication authentication) {
        return bookingService.getMyBookings(authentication);
    }

    @Operation(summary = "View bookings made on your cars (agency only)")
    @GetMapping("/my-cars")
    @PreAuthorize("hasRole('AGENCY')")
    public List<BookingResponse> getBookingForMyCars(Authentication authentication) {
        return bookingService.getBookingForMyCars(authentication);
    }

    @Operation(summary = "Approve a pending booking (car owner/agency only)")
    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('AGENCY')")
    public BookingResponse approveBooking(@PathVariable("id") Long id,
                                          Authentication authentication) {
        return bookingService.approveBooking(id, authentication);
    }

    @Operation(summary = "Reject a pending booking (car owner/agency only)")
    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('AGENCY')")
    public BookingResponse rejectBooking(@PathVariable("id") Long id,
                                         Authentication authentication) {
        return bookingService.rejectBooking(id, authentication);
    }

    @Operation(summary = "Cancel a booking (the renter or the car owner)")
    @PreAuthorize("hasAnyRole('CUSTOMER','AGENCY')")
    @PatchMapping("/{id}/cancel")
    public BookingResponse cancelBooking(@PathVariable("id") Long id,
                                         Authentication authentication) {
        return bookingService.cancelBooking(id, authentication);
    }

    @Operation(summary = "List all bookings, optionally filtered by status (admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public PageResponse<BookingResponse> getAllBookings(Authentication authentication,
                                                        @RequestParam(required = false) String status,
                                                        @PageableDefault(size = 10) Pageable pageable) {
        return bookingService.getAllBookings(authentication, status, pageable);
    }
}