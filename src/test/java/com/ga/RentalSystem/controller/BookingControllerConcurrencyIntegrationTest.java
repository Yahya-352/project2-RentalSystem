package com.ga.RentalSystem.controller;

import com.ga.RentalSystem.dto.request.LoginRequest;
import com.ga.RentalSystem.dto.response.LoginResponse;
import com.ga.RentalSystem.enums.BookingStatus;
import com.ga.RentalSystem.enums.Role;
import com.ga.RentalSystem.enums.UserStatus;
import com.ga.RentalSystem.model.*;
import com.ga.RentalSystem.repository.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookingControllerConcurrencyIntegrationTest {

    private static final String AGENCY_EMAIL = "agency.concurrency@test.com";
    private static final String CUSTOMER_EMAIL = "customer.concurrency@test.com";
    private static final String RAW_PASSWORD = "Password123!";
    private static final int N = 10;

    @Autowired
    private MockMvc mockMvc;
    private List<Long> bookingIds;
    private String token;

    private Make make;
    private Category category;
    private User agency;
    private User customer;
    private Car car;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private MakeRepository makeRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private CarRepository carRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception{
        make = new Make();
        make.setName("ConcurrencyTestMake");
        make = makeRepository.save(make);

        category = new Category();
        category.setName("ConcurrencyTestCategory");
        category = categoryRepository.save(category);

        agency = userRepository.save(newUser("concurrency_agency", AGENCY_EMAIL, Role.AGENCY));
        customer = userRepository.save(newUser("concurrency_customer", CUSTOMER_EMAIL, Role.CUSTOMER));

        car = new Car();
        car.setMake(make);
        car.setCategory(category);
        car.setOwner(agency);
        car.setModel("Test Model");
        car.setPricePerDay(new BigDecimal("20.000"));
        car = carRepository.save(car);

        // N pending bookings on the same car and the same dates, so they compete for it
        bookingIds = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            Booking booking = new Booking();
            booking.setRenter(customer);
            booking.setCar(car);
            booking.setStartDate(LocalDate.now().plusDays(10));
            booking.setEndDate(LocalDate.now().plusDays(12));
            booking.setTotalPrice(new BigDecimal("40.000"));
            booking.setStatus(BookingStatus.PENDING);
            bookingIds.add(bookingRepository.save(booking).getId());
        }

        token = login(AGENCY_EMAIL , RAW_PASSWORD);
    }

    private User newUser(String userName, String email, Role role) {
        User user = new User();
        user.setUserName(userName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(RAW_PASSWORD));
        user.setRoleEnum(role);
        user.setUserStatus(UserStatus.ACTIVE);
        user.setVerified(true);
        return user;
    }

    @AfterEach
    void cleanUp() {
        // only what setUp created, children before parents because of the foreign keys
        bookingRepository.deleteAll(bookingRepository.findByCarId(car.getId()));
        carRepository.delete(car);
        makeRepository.delete(make);
        categoryRepository.delete(category);
        userRepository.delete(customer);
        userRepository.delete(agency);
    }

    String login(String email, String password) throws Exception {
        String json = objectMapper.writeValueAsString(new LoginRequest(email , password));
        String response = mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        LoginResponse responseObject = objectMapper.readValue(response, LoginResponse.class);
        return responseObject.token();
    }

    @Test
    void approveBooking() throws Exception {
        ExecutorService service = Executors.newFixedThreadPool(N);
        CountDownLatch ready = new CountDownLatch(N);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> futures = new ArrayList<>();
        List<Integer> statuses = new ArrayList<>();

        try {
            for (int i = 0; i < N; i++) {
                Long id = bookingIds.get(i);
                futures.add(service.submit(() -> {
                    ready.countDown();
                    start.await();
                    int status = mockMvc.perform(patch("/bookings/{id}/approve" , id)
                                    .header("Authorization" , "Bearer " + token))
                            .andReturn().getResponse().getStatus();
                    System.out.println(Thread.currentThread().getName()
                            + " -> booking " + id + " -> " + status);
                    return status;
                }));
            }
            ready.await();
            start.countDown();

            for (Future<Integer> future : futures) {
                statuses.add(future.get(10, TimeUnit.SECONDS));
            }
        } finally {
            service.shutdown();
        }

        assertEquals(1, Collections.frequency(statuses, 200),
                "statuses: " + statuses);
        assertEquals(N - 1, Collections.frequency(statuses, 409),
                "statuses: " + statuses);
    }

    @Test
    void getBookingsForMyCars() throws Exception {
        mockMvc.perform(get("/bookings/my-cars")
                .header("Authorization" , "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(N));
    }
}
