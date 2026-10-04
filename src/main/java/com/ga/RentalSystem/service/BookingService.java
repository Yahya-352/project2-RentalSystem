package com.ga.RentalSystem.service;

import com.ga.RentalSystem.dto.request.BookingRequest;
import com.ga.RentalSystem.dto.response.BookingResponse;
import com.ga.RentalSystem.enums.BookingStatus;
import com.ga.RentalSystem.exceptions.BadRequestException;
import com.ga.RentalSystem.exceptions.ConflictException;
import com.ga.RentalSystem.exceptions.InformationNotFoundException;
import com.ga.RentalSystem.model.Booking;
import com.ga.RentalSystem.model.Car;
import com.ga.RentalSystem.model.User;
import com.ga.RentalSystem.repository.BookingRepository;
import com.ga.RentalSystem.repository.CarRepository;
import com.ga.RentalSystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {
    private final BookingRepository bookingRepository;
    private final CarRepository carRepository;
    private final UserRepository userRepository;

    public BookingResponse createBooking(BookingRequest request , Authentication authentication){
        User renter = getCurrentUser(authentication);

        Car car = carRepository.findById(request.carId())
                .orElseThrow(() -> new InformationNotFoundException("Car not found"));

        if (!request.startDate().isBefore(request.endDate())) {
            throw new BadRequestException("Start date must be before end date");
        }

        if (request.startDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Booking cannot start in the past");
        }

        if(!isCarAvailable(request.carId(), request.startDate() , request.endDate())){
            throw new ConflictException("Car is not available for that specific date");
        }

        long days = ChronoUnit.DAYS.between(request.startDate(), request.endDate());
        BigDecimal totalPrice = car.getPricePerDay().multiply(BigDecimal.valueOf(days));

        Booking booking = new Booking();
        booking.setRenter(renter);
        booking.setCar(car);
        booking.setStartDate(request.startDate());
        booking.setEndDate(request.endDate());
        booking.setTotalPrice(totalPrice);
        booking.setStatus(BookingStatus.PENDING);

        Booking saved = bookingRepository.save(booking);
        return toResponse(saved);
    }

    public List<BookingResponse> getMyBookings(Authentication authentication){
        User currentUser = getCurrentUser(authentication);
        List<Booking> bookings = bookingRepository.findByRenterId(currentUser.getId());
        return bookings.stream().map(booking ->toResponse(booking)).toList();
    }

    public List<BookingResponse> getBookingForMyCars(Authentication authentication){
        User currentUser = getCurrentUser(authentication);
        List<Booking> bookings = bookingRepository.findByRenterId(currentUser.getId());
        return bookings.stream().map(booking ->toResponse(booking)).toList();
    }

    public boolean isCarAvailable(Long carId , LocalDate startDate , LocalDate endDate){
        List<Booking> bookings = bookingRepository.findByCarId(carId);
        boolean overlap = bookings.stream()
                .filter(b -> b.getStatus().equals(BookingStatus.APPROVED))
                .noneMatch(b -> startDate.isBefore(b.getEndDate())
                && endDate.isAfter(b.getStartDate()));
        return overlap;
    }

    private User getCurrentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new InformationNotFoundException("User not found"));
    }

    private BookingResponse toResponse(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getCar().getId(),
                booking.getCar().getMake(),
                booking.getCar().getModel(),
                booking.getRenter().getId(),
                booking.getStartDate(),
                booking.getEndDate(),
                booking.getTotalPrice(),
                booking.getStatus()
        );
    }

}
