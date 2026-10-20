package com.ga.RentalSystem.service;

import com.ga.RentalSystem.enums.BookingStatus;
import com.ga.RentalSystem.model.Booking;
import com.ga.RentalSystem.model.Car;
import com.ga.RentalSystem.model.Make;
import com.ga.RentalSystem.model.User;
import com.ga.RentalSystem.repository.BookingRepository;
import com.ga.RentalSystem.repository.CarRepository;
import com.ga.RentalSystem.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.awt.print.Book;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class BookingConcurrencyTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private CarRepository carRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private AuditLogService auditLogService;
    @Mock private Authentication authentication;

    @InjectMocks
    private BookingService bookingService;

    private User createUser(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }
    private Car createCar(User agency) {
        Make make = new Make();
        make.setName("Toyota");

        Car car = new Car();
        car.setId(1L);
        car.setMake(make);
        car.setModel("Camry");
        car.setYear(2018);
        car.setAvailable(true);
        car.setOwner(agency);
        return car;
    }
    private Booking createBooking(Long id, Car car , User renter ,
                                  LocalDate startDate, LocalDate endDate) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setCar(car);
        booking.setRenter(renter);
        booking.setStartDate(startDate);
        booking.setStatus(BookingStatus.PENDING);
        booking.setEndDate(endDate);
        return booking;
    }

    //this method showwcases 2 threads accessing same booking at the same time
    @Test
    void approveBooking_twoOverlappingApprovalsAtTheSameTime_onlyOneIsApproved() throws Exception {
        User user = createUser(1L);
        User agency = createUser(2L);
        Car car = createCar(agency);
        Booking bookingA = createBooking(10L, car, user,
                LocalDate.now(), LocalDate.now().plusDays(3));
        Booking bookingB = createBooking(11L , car , user ,
                LocalDate.now(), LocalDate.now().plusDays(3));
        when(authentication.getName()).thenReturn("agency@rental.com");
        when(userRepository.findByEmail("agency@rental.com")).thenReturn(Optional.of(agency));
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(bookingA));
        when(bookingRepository.findById(11L)).thenReturn(Optional.of(bookingB));
        when(bookingRepository.findByCarId(car.getId())).thenAnswer( (invoke) ->{
            List<Booking> snapshot = List.of(copyOfBooking(bookingA)
                        , copyOfBooking(bookingB));
                Thread.sleep(500);
                return snapshot;
        });
        when(bookingRepository.save(any(Booking.class))).thenAnswer(
                invoke -> {
                    Booking saved =  invoke.getArgument(0);
                    if (saved.getId().equals(bookingA.getId())) {
                        bookingA.setStatus(saved.getStatus());
                    }else if (saved.getId().equals(bookingB.getId())) {
                        bookingB.setStatus(saved.getStatus());
                    }
                    return saved;
                }
        );
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        AtomicInteger counter = new AtomicInteger(0);
        executorService.submit(() -> {
            bookingService.approveBooking(bookingA.getId() , authentication);
            counter.incrementAndGet();
        });
        executorService.submit(() -> {
            bookingService.approveBooking(bookingB.getId() , authentication);
            counter.incrementAndGet();
        });
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);
        assertEquals(1 , counter.get());
    }

    private Booking copyOfBooking(Booking booking) {
        Booking copy = new Booking();
        copy.setId(booking.getId());
        copy.setCar(booking.getCar());
        copy.setRenter(booking.getRenter());
        copy.setStartDate(booking.getStartDate());
        copy.setStatus(booking.getStatus());
        copy.setEndDate(booking.getEndDate());
        return copy;
    }
}
