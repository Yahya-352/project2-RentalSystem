package com.ga.booking_service.service;

import com.ga.booking_service.client.BookingEventPublisher;
import com.ga.booking_service.client.CarClient;
import com.ga.booking_service.client.CarSummary;
import com.ga.booking_service.client.PaymentClient;
import com.ga.booking_service.dto.request.BookingRequest;
import com.ga.booking_service.dto.response.BookingResponse;
import com.ga.booking_service.enums.BookingStatus;
import com.ga.booking_service.model.Booking;
import com.ga.booking_service.repository.BookingRepository;
import com.ga.common.exceptions.BadRequestException;
import com.ga.common.exceptions.ConflictException;
import com.ga.common.exceptions.ForbiddenException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    private static final String AUTH_HEADER = "Bearer token";

    @Mock private BookingRepository bookingRepository;
    @Mock private CarClient carClient;
    @Mock private PaymentClient paymentClient;
    @Mock private BookingEventPublisher bookingEventPublisher;
    @Mock private Authentication authentication;

    @InjectMocks
    private BookingService bookingService;

    private CarSummary createCar(Long ownerId) {
        return new CarSummary(1L, ownerId, new BigDecimal("100"), true, "Toyota", "Camry");
    }

    @Test
    void createBooking_startDateAfterEndDate_throwsBadRequest() {
        when(authentication.getPrincipal()).thenReturn(2L);
        when(carClient.getCar(1L, AUTH_HEADER)).thenReturn(createCar(1L));

        BookingRequest request = new BookingRequest(1L,
                LocalDate.now().plusDays(5), LocalDate.now().plusDays(2));

        assertThrows(BadRequestException.class,
                () -> bookingService.createBooking(request, authentication, AUTH_HEADER));
    }

    @Test
    void createBooking_threeDays_calculatesTotalPrice() {
        when(authentication.getPrincipal()).thenReturn(2L);
        when(carClient.getCar(1L, AUTH_HEADER)).thenReturn(createCar(1L));
        when(bookingRepository.findByCarId(1L)).thenReturn(List.of());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        BookingRequest request = new BookingRequest(1L,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(4));

        BookingResponse response = bookingService.createBooking(request, authentication, AUTH_HEADER);

        assertEquals(0, new BigDecimal("300").compareTo(response.totalPrice()));
        assertEquals(BookingStatus.PENDING, response.status());
    }

    @Test
    void createBooking_overlapsApprovedBooking_throwsConflict() {
        Booking approved = new Booking();
        approved.setStatus(BookingStatus.APPROVED);
        approved.setStartDate(LocalDate.now().plusDays(1));
        approved.setEndDate(LocalDate.now().plusDays(5));

        when(authentication.getPrincipal()).thenReturn(2L);
        when(carClient.getCar(1L, AUTH_HEADER)).thenReturn(createCar(1L));
        when(bookingRepository.findByCarId(1L)).thenReturn(List.of(approved));

        BookingRequest request = new BookingRequest(1L,
                LocalDate.now().plusDays(3), LocalDate.now().plusDays(7));

        assertThrows(ConflictException.class,
                () -> bookingService.createBooking(request, authentication, AUTH_HEADER));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void approveBooking_notTheCarOwner_throwsForbidden() {
        Booking booking = new Booking();
        booking.setId(10L);
        booking.setCarId(1L);
        booking.setOwnerId(1L);
        booking.setStatus(BookingStatus.PENDING);

        when(authentication.getPrincipal()).thenReturn(3L);
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        assertThrows(ForbiddenException.class,
                () -> bookingService.approveBooking(10L, authentication));
    }

    @Test
    void cancelBooking_byRenter_setsStatusCancelled() {
        Booking booking = new Booking();
        booking.setId(10L);
        booking.setCarId(1L);
        booking.setOwnerId(1L);
        booking.setRenterId(2L);
        booking.setStatus(BookingStatus.PENDING);

        when(authentication.getPrincipal()).thenReturn(2L);
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        bookingService.cancelBooking(10L, authentication);

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
    }


}
