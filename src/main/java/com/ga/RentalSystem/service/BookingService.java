package com.ga.RentalSystem.service;

import com.ga.RentalSystem.dto.request.BookingRequest;
import com.ga.RentalSystem.dto.response.BookingResponse;
import com.ga.RentalSystem.dto.response.PageResponse;
import com.ga.RentalSystem.enums.BookingStatus;

import com.ga.RentalSystem.exceptions.BadRequestException;
import com.ga.RentalSystem.exceptions.ConflictException;
import com.ga.RentalSystem.exceptions.ForbiddenException;
import com.ga.RentalSystem.exceptions.InformationNotFoundException;
import com.ga.RentalSystem.model.Booking;
import com.ga.RentalSystem.model.Car;
import com.ga.RentalSystem.model.User;
import com.ga.RentalSystem.repository.BookingRepository;
import com.ga.RentalSystem.repository.CarRepository;
import com.ga.RentalSystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Business logic for car bookings.
 * <p>
 * A customer requests a booking, which starts as {@code PENDING}. The agency that owns the
 * car then approves or rejects it, and either side can cancel it. A car cannot have two
 * approved bookings for overlapping dates. Booking events are sent to the other party as
 * real-time notifications, and every important change is written to the audit log.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {
    private final BookingRepository bookingRepository;
    private final CarRepository carRepository;
    private final UserRepository userRepository;

    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    /**
     * Creates a booking request for a car. The total price is the car's price per day
     * multiplied by the number of days. The booking starts as {@code PENDING}, the car's
     * owner is notified, and the action is written to the audit log.
     *
     * @param request        the car id, start date and end date
     * @param authentication the logged-in user, who becomes the renter
     * @return the created booking
     * @throws InformationNotFoundException if the user or car does not exist, or the car is
     *                                      deleted or unavailable
     * @throws BadRequestException          if the start date is not before the end date, or the
     *                                      start date is in the past
     * @throws ConflictException            if an approved booking already covers any of the dates
     */
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

    /**
     * Returns every booking made by the logged-in user.
     *
     * @param authentication the logged-in user
     * @return the user's bookings
     * @throws InformationNotFoundException if the user does not exist
     */
    public List<BookingResponse> getMyBookings(Authentication authentication){
        User currentUser = getCurrentUser(authentication);
        List<Booking> bookings = bookingRepository.findByRenterId(currentUser.getId());
        return bookings.stream().map(booking ->toResponse(booking)).toList();
    }

    /**
     * Returns every booking made on the cars owned by the logged-in user.
     *
     * @param authentication the logged-in user, who is the car owner
     * @return the bookings on the user's cars
     * @throws InformationNotFoundException if the user does not exist
     */
    public List<BookingResponse> getBookingForMyCars(Authentication authentication){
        User currentUser = getCurrentUser(authentication);
        List<Booking> bookings = bookingRepository.findByCarOwnerId(currentUser.getId());
        return bookings.stream().map(booking ->toResponse(booking)).toList();
    }

    /**
     * Approves a pending booking on one of the logged-in user's cars.
     * Availability is checked again at approval time, because another booking may have been
     * approved for the same dates in the meantime. The renter is notified and the action is
     * written to the audit log.
     *
     * @param bookingId      the id of the booking to approve
     * @param authentication the logged-in user, who must own the car
     * @return the approved booking
     * @throws InformationNotFoundException if the user or booking does not exist
     * @throws ForbiddenException           if the car belongs to another user
     * @throws BadRequestException          if the booking is not pending
     * @throws ConflictException            if an approved booking now covers the same dates
     */
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

    /**
     * Rejects a pending booking on one of the logged-in user's cars.
     * The renter is notified and the action is written to the audit log.
     *
     * @param bookingId      the id of the booking to reject
     * @param authentication the logged-in user, who must own the car
     * @return the rejected booking
     * @throws InformationNotFoundException if the user or booking does not exist
     * @throws ForbiddenException           if the car belongs to another user
     * @throws BadRequestException          if the booking is not pending
     */
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

    /**
     * Cancels a booking. Either the renter or the owner of the car can cancel it, as long
     * as it is not already cancelled, rejected or completed. The action is written to the
     * audit log.
     *
     * @param bookingId      the id of the booking to cancel
     * @param authentication the logged-in user, who must be the renter or the car owner
     * @return the cancelled booking
     * @throws InformationNotFoundException if the user or booking does not exist
     * @throws ForbiddenException           if the user is neither the renter nor the car owner
     * @throws BadRequestException          if the booking is already cancelled, rejected or
     *                                      completed
     */
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

    /**
     * Checks whether a car is free for a date range. Only approved bookings block a car;
     * pending, rejected and cancelled bookings are ignored. Two ranges overlap when each
     * one starts before the other ends.
     *
     * @param carId     the id of the car
     * @param startDate the first day of the requested range
     * @param endDate   the last day of the requested range
     * @return {@code true} if no approved booking overlaps the range, otherwise {@code false}
     */
    public boolean isCarAvailable(Long carId , LocalDate startDate , LocalDate endDate){
        List<Booking> bookings = bookingRepository.findByCarId(carId);
        boolean overlap = bookings.stream()
                .filter(b -> b.getStatus().equals(BookingStatus.APPROVED))
                .noneMatch(b -> startDate.isBefore(b.getEndDate())
                        && endDate.isAfter(b.getStartDate()));
        return overlap;
    }

    /**
     * Finds the logged-in user in the database.
     *
     * @param authentication the logged-in user
     * @return the user entity
     * @throws InformationNotFoundException if the user does not exist
     */
    private User getCurrentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new InformationNotFoundException("User not found"));
    }

    /**
     * Returns a page of all bookings, optionally filtered by status.
     * Only admins can reach this method, which is enforced in the controller.
     *
     * @param authentication the logged-in user
     * @param status         the booking status to filter by (case-insensitive), or {@code null}
     *                       for all bookings
     * @param pageable       the page number, page size and sorting
     * @return a page of bookings
     * @throws InformationNotFoundException if the user does not exist
     * @throws BadRequestException          if the status is not a valid booking status
     */
    public PageResponse<BookingResponse> getAllBookings(Authentication authentication ,
                                                        String status , Pageable pageable) {
        User currentUser = getCurrentUser(authentication);

        Page<Booking> bookings;

        if (status != null) {
            BookingStatus bookingStatus;
            try {
                bookingStatus = BookingStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid booking status: " + status);
            }
            bookings = bookingRepository.findByStatus(bookingStatus, pageable);
        } else {
            bookings = bookingRepository.findAll(pageable);
        }

        Page<BookingResponse> responsePage = bookings.map(booking -> toResponse(booking));
        return PageResponse.from(responsePage);
    }

    /**
     * Converts a {@link Booking} entity into a {@link BookingResponse}, so every method
     * returns the same shape.
     *
     * @param booking the booking entity
     * @return the response object
     */
    private BookingResponse toResponse(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getCar().getId(),
                booking.getCar().getMake().getName(),
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