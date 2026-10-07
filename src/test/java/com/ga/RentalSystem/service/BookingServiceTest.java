//package com.ga.RentalSystem.service;
//
//import com.ga.RentalSystem.dto.request.BookingRequest;
//import com.ga.RentalSystem.dto.response.BookingResponse;
//import com.ga.RentalSystem.enums.BookingStatus;
//import com.ga.RentalSystem.exceptions.BadRequestException;
//import com.ga.RentalSystem.exceptions.ConflictException;
//import com.ga.RentalSystem.exceptions.ForbiddenException;
//import com.ga.RentalSystem.model.Booking;
//import com.ga.RentalSystem.model.Car;
//import com.ga.RentalSystem.model.Make;
//import com.ga.RentalSystem.model.User;
//import com.ga.RentalSystem.repository.BookingRepository;
//import com.ga.RentalSystem.repository.CarRepository;
//import com.ga.RentalSystem.repository.UserRepository;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.security.core.Authentication;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.util.List;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.never;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//
//@ExtendWith(MockitoExtension.class)
//class BookingServiceTest {
//
//    @Mock private BookingRepository bookingRepository;
//    @Mock private CarRepository carRepository;
//    @Mock private UserRepository userRepository;
//    @Mock private NotificationService notificationService;
//    @Mock private AuditLogService auditLogService;
//    @Mock private Authentication authentication;
//
//    @InjectMocks
//    private BookingService bookingService;
//
//    private User createUser(Long id) {
//        User user = new User();
//        user.setId(id);
//        return user;
//    }
//
//    private Car createCar(User owner) {
//        Make make = new Make();
//        make.setName("Toyota");
//
//        Car car = new Car();
//        car.setId(1L);
//        car.setMake(make);
//        car.setModel("Camry");
//        car.setPricePerDay(new BigDecimal("100"));
//        car.setAvailable(true);
//        car.setDeleted(false);
//        car.setOwner(owner);
//        return car;
//    }
//
//    @Test
//    void createBooking_startDateAfterEndDate_throwsBadRequest() {
//        User renter = createUser(2L);
//        Car car = createCar(createUser(1L));
//
//        when(authentication.getName()).thenReturn("renter@rental.com");
//        when(userRepository.findByEmail("renter@rental.com")).thenReturn(Optional.of(renter));
//        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
//
//        BookingRequest request = new BookingRequest(1L,
//                LocalDate.now().plusDays(5), LocalDate.now().plusDays(2));
//
//        assertThrows(BadRequestException.class,
//                () -> bookingService.createBooking(request, authentication));
//    }
//
//    @Test
//    void createBooking_threeDays_calculatesTotalPrice() {
//        User renter = createUser(2L);
//        Car car = createCar(createUser(1L));
//
//        when(authentication.getName()).thenReturn("renter@rental.com");
//        when(userRepository.findByEmail("renter@rental.com")).thenReturn(Optional.of(renter));
//        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
//        when(bookingRepository.findByCarId(1L)).thenReturn(List.of());
//        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));
//
//        BookingRequest request = new BookingRequest(1L,
//                LocalDate.now().plusDays(1), LocalDate.now().plusDays(4));
//
//        BookingResponse response = bookingService.createBooking(request, authentication);
//
//        assertEquals(0, new BigDecimal("300").compareTo(response.totalPrice()));
//        assertEquals(BookingStatus.PENDING, response.status());
//    }
//
//    @Test
//    void createBooking_overlapsApprovedBooking_throwsConflict() {
//        User renter = createUser(2L);
//        Car car = createCar(createUser(1L));
//
//        Booking approved = new Booking();
//        approved.setStatus(BookingStatus.APPROVED);
//        approved.setStartDate(LocalDate.now().plusDays(1));
//        approved.setEndDate(LocalDate.now().plusDays(5));
//
//        when(authentication.getName()).thenReturn("renter@rental.com");
//        when(userRepository.findByEmail("renter@rental.com")).thenReturn(Optional.of(renter));
//        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
//        when(bookingRepository.findByCarId(1L)).thenReturn(List.of(approved));
//
//        BookingRequest request = new BookingRequest(1L,
//                LocalDate.now().plusDays(3), LocalDate.now().plusDays(7));
//
//        assertThrows(ConflictException.class,
//                () -> bookingService.createBooking(request, authentication));
//        verify(bookingRepository, never()).save(any(Booking.class));
//    }
//
//    @Test
//    void approveBooking_notTheCarOwner_throwsForbidden() {
//        User owner = createUser(1L);
//        User otherAgency = createUser(3L);
//
//        Booking booking = new Booking();
//        booking.setId(10L);
//        booking.setCar(createCar(owner));
//        booking.setStatus(BookingStatus.PENDING);
//
//        when(authentication.getName()).thenReturn("other@rental.com");
//        when(userRepository.findByEmail("other@rental.com")).thenReturn(Optional.of(otherAgency));
//        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
//
//        assertThrows(ForbiddenException.class,
//                () -> bookingService.approveBooking(10L, authentication));
//    }
//
//    @Test
//    void cancelBooking_byRenter_setsStatusCancelled() {
//        User owner = createUser(1L);
//        User renter = createUser(2L);
//
//        Booking booking = new Booking();
//        booking.setId(10L);
//        booking.setCar(createCar(owner));
//        booking.setRenter(renter);
//        booking.setStatus(BookingStatus.PENDING);
//
//        when(authentication.getName()).thenReturn("renter@rental.com");
//        when(userRepository.findByEmail("renter@rental.com")).thenReturn(Optional.of(renter));
//        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
//        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));
//
//        bookingService.cancelBooking(10L, authentication);
//
//        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
//    }
//}