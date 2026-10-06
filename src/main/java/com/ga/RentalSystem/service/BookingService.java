package com.ga.RentalSystem.service;

import com.ga.RentalSystem.dto.request.BookingRequest;
import com.ga.RentalSystem.dto.response.BookingResponse;
import com.ga.RentalSystem.dto.response.PageResponse;
import com.ga.RentalSystem.enums.BookingStatus;
import com.ga.RentalSystem.enums.Role;
import com.ga.RentalSystem.exceptions.BadRequestException;
import com.ga.RentalSystem.exceptions.ConflictException;
import com.ga.RentalSystem.exceptions.ForbiddenException;
import com.ga.RentalSystem.exceptions.InformationNotFoundException;
import com.ga.RentalSystem.model.Booking;
import com.ga.RentalSystem.model.Car;
import com.ga.RentalSystem.model.User;
import com.ga.RentalSystem.repository.AuditLogRepository;
import com.ga.RentalSystem.repository.BookingRepository;
import com.ga.RentalSystem.repository.CarRepository;
import com.ga.RentalSystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {
    private final BookingRepository bookingRepository;
    private final CarRepository carRepository;
    private final UserRepository userRepository;

    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public BookingResponse createBooking(BookingRequest request , Authentication authentication){
        User renter = getCurrentUser(authentication);

        Car car = carRepository.findById(request.carId())
                .orElseThrow(() -> new InformationNotFoundException("Car not found"));

        if(car.isDeleted() || !car.isAvailable()){
            throw new InformationNotFoundException("Car not found");
        }

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

        BookingResponse response = toResponse(saved);
        //notification
        notificationService.sendEvent(car.getOwner().getId(), "BOOKING_CREATED", response);

        //logging
        String message = "Booking " + booking.getId() + " created by user " + renter.getId();
        log.info(message);
        auditLogService.log(renter.getId(), "BOOKING_CREATED", "Booking", saved.getId(), message);
        return response;

    }

    public List<BookingResponse> getMyBookings(Authentication authentication){
        User currentUser = getCurrentUser(authentication);
        List<Booking> bookings = bookingRepository.findByRenterId(currentUser.getId());
        return bookings.stream().map(booking ->toResponse(booking)).toList();
    }

    public List<BookingResponse> getBookingForMyCars(Authentication authentication){
        User currentUser = getCurrentUser(authentication);
        List<Booking> bookings = bookingRepository.findByCarOwnerId(currentUser.getId());
        return bookings.stream().map(booking ->toResponse(booking)).toList();
    }

    public BookingResponse approveBooking(Long bookingId,Authentication authentication){
        User currentUser = getCurrentUser(authentication);
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() ->
                new InformationNotFoundException("Booking Not Found"));

        if (!booking.getCar().getOwner().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You do not own this car");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Only pending bookings can be approved");
        }
        if(isCarAvailable(booking.getCar().getId()
                , booking.getStartDate() , booking.getEndDate())){

            booking.setStatus(BookingStatus.APPROVED);
            BookingResponse response = toResponse(bookingRepository.save(booking));

            String message = "Booking " + booking.getId() + " approved by Agency " + currentUser.getId();
            log.info(message);
            auditLogService.log(currentUser.getId(), "BOOKING_APPROVED", "Booking", booking.getId(), message);

            notificationService.sendEvent(booking.getRenter().getId(), "BOOKING_APPROVED", response);

            return response;
        }else{
            throw new ConflictException("Car is not available at this date");
        }
    }

    public BookingResponse rejectBooking(Long bookingId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() ->
                new InformationNotFoundException("Booking Not Found"));

        if (!booking.getCar().getOwner().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You do not own this car");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Only pending bookings can be rejected");
        }

        booking.setStatus(BookingStatus.REJECTED);

        BookingResponse response = toResponse(bookingRepository.save(booking));


        String message = "Booking " + booking.getId() + " Rejected by Agency " + currentUser.getId();
        log.info(message);
        auditLogService.log(currentUser.getId(), "BOOKING_REJECTED", "Booking", booking.getId(), message);


        notificationService.sendEvent(booking.getRenter().getId(), "BOOKING_REJECTED", response);
        return response;
    }

    public BookingResponse cancelBooking(Long bookingId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() ->
                new InformationNotFoundException("Booking Not Found"));

        boolean isRenter = booking.getRenter().getId().equals(currentUser.getId());
        boolean isOwner = booking.getCar().getOwner().getId().equals(currentUser.getId());

        if (!isRenter && !isOwner) {
            throw new ForbiddenException("You are not involved in this booking");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED
                || booking.getStatus() == BookingStatus.REJECTED
                || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BadRequestException("This booking cannot be cancelled");
        }

        String message = "Booking " + booking.getId() + " cancelled by user " + currentUser.getId();
        log.info(message);
        auditLogService.log(currentUser.getId(), "BOOKING_CANCELLED", "Booking", booking.getId(), message);

        booking.setStatus(BookingStatus.CANCELLED);
        return toResponse(bookingRepository.save(booking));
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

    public PageResponse<BookingResponse> getAllBookings(Authentication authentication , Pageable pageable) {
        User currentUser = getCurrentUser(authentication);
        if (currentUser.getRoleEnum() != Role.ADMIN) {
            throw new ForbiddenException("Admin access required");
        }
        Page<Booking> bookings = bookingRepository.findAll(pageable);
        Page<BookingResponse> responsePage = bookings.map(booking -> toResponse(booking));
        return PageResponse.from(responsePage);
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
                booking.getStatus(),
                booking.getCreatedAt(),
                booking.getUpdatedAt()
        );
    }

}
