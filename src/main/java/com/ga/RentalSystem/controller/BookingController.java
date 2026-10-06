package com.ga.RentalSystem.controller;

import com.ga.RentalSystem.dto.request.BookingRequest;
import com.ga.RentalSystem.dto.response.BookingResponse;
import com.ga.RentalSystem.dto.response.PageResponse;
import com.ga.RentalSystem.service.BookingService;
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

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<BookingResponse> createBooking(@RequestBody @Valid BookingRequest request,
                                                         Authentication authentication) {
        BookingResponse response = bookingService.createBooking(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<BookingResponse> getMyBookings(Authentication authentication) {
        return bookingService.getMyBookings(authentication);
    }


    @GetMapping("/my-cars")
    @PreAuthorize("hasRole('AGENCY')")
    public List<BookingResponse> getBookingForMyCars(Authentication authentication) {
        return bookingService.getBookingForMyCars(authentication);
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('AGENCY')")
    public BookingResponse approveBooking(@PathVariable("id") Long id,
                                          Authentication authentication) {
        return bookingService.approveBooking(id, authentication);
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('AGENCY')")
    public BookingResponse rejectBooking(@PathVariable("id") Long id,
                                         Authentication authentication) {
        return bookingService.rejectBooking(id, authentication);
    }

    @PreAuthorize("hasAnyRole('CUSTOMER','AGENCY')")
    @PatchMapping("/{id}/cancel")
    public BookingResponse cancelBooking(@PathVariable("id") Long id,
                                         Authentication authentication) {
        return bookingService.cancelBooking(id, authentication);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public PageResponse<BookingResponse> getAllBookings(Authentication authentication,
                                                        @RequestParam(required = false) String status,
                                                        @PageableDefault(size = 10) Pageable pageable) {
        return bookingService.getAllBookings(authentication, status, pageable);
    }
}
