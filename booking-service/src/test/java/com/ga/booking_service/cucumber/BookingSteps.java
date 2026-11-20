package com.ga.booking_service.cucumber;

import com.ga.booking_service.client.BookingEventPublisher;
import com.ga.booking_service.client.CarClient;
import com.ga.booking_service.client.CarSummary;
import com.ga.booking_service.client.PaymentClient;
import com.ga.booking_service.dto.request.BookingRequest;
import com.ga.booking_service.dto.response.BookingResponse;
import com.ga.booking_service.enums.BookingStatus;
import com.ga.booking_service.model.Booking;
import com.ga.booking_service.repository.BookingRepository;
import com.ga.booking_service.service.BookingService;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Step definitions for booking.feature. The real BookingService runs against mocks of its
 * neighbours: the repository is an in-memory map, and car-service, payment-service and the
 * Kafka publisher are Mockito mocks, so the scenarios need no database, Kafka or other service.
 */
public class BookingSteps {

    private static final String AUTH_HEADER = "Bearer token";

    private BookingRepository bookingRepository;
    private CarClient carClient;
    private PaymentClient paymentClient;
    private BookingEventPublisher bookingEventPublisher;
    private BookingService bookingService;

    private final Map<Long, Booking> bookings = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1000);
    private volatile boolean slowReads;

    private BookingResponse lastResponse;
    private Exception lastError;
    private Long ownerId;
    private BigDecimal pricePerDay;

    @Before
    public void setUp() {
        bookingRepository = mock(BookingRepository.class);
        carClient = mock(CarClient.class);
        paymentClient = mock(PaymentClient.class);
        bookingEventPublisher = mock(BookingEventPublisher.class);
        bookingService = new BookingService(bookingRepository, bookingEventPublisher, carClient, paymentClient);

        when(bookingRepository.findById(anyLong()))
                .thenAnswer(i -> Optional.ofNullable(bookings.get(i.<Long>getArgument(0))));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> {
            Booking booking = i.getArgument(0);
            if (booking.getId() == null) {
                booking.setId(nextId.incrementAndGet());
            }
            bookings.put(booking.getId(), booking);
            return booking;
        });
        // returns copies taken at call time, like rows read from a database
        when(bookingRepository.findByCarId(anyLong())).thenAnswer(i -> {
            Long carId = i.getArgument(0);
            List<Booking> snapshot = bookings.values().stream()
                    .filter(b -> b.getCarId().equals(carId))
                    .map(BookingSteps::copy)
                    .toList();
            if (slowReads) {
                Thread.sleep(300);
            }
            return snapshot;
        });
    }

    @Given("agency {long} owns car {long} priced at {int} per day")
    public void agencyOwnsCar(Long agencyId, Long carId, int price) {
        ownerId = agencyId;
        pricePerDay = BigDecimal.valueOf(price);
        when(carClient.getCar(eq(carId), anyString()))
                .thenReturn(new CarSummary(carId, agencyId, pricePerDay, true, "Toyota", "Corolla"));
    }

    @Given("customer {long} is logged in")
    public void customerIsLoggedIn(Long customerId) {
        assertNotNull(customerId);
    }

    @Given("car {long} is marked unavailable")
    public void carIsUnavailable(Long carId) {
        when(carClient.getCar(eq(carId), anyString()))
                .thenReturn(new CarSummary(carId, ownerId, pricePerDay, false, "Toyota", "Corolla"));
    }

    @Given("car {long} has an approved booking from {int} to {int} days from now")
    public void carHasApprovedBooking(Long carId, int start, int end) {
        Booking booking = booking(nextId.incrementAndGet(), 99L, carId, start, end, BookingStatus.APPROVED);
        bookings.put(booking.getId(), booking);
    }

    @Given("customer {long} has a pending booking {long} for car {long} from {int} to {int} days from now")
    public void customerHasPendingBooking(Long customerId, Long bookingId, Long carId, int start, int end) {
        bookings.put(bookingId, booking(bookingId, customerId, carId, start, end, BookingStatus.PENDING));
    }

    @Given("customer {long} has a paid approved booking {long} for car {long} from {int} to {int} days from now")
    public void customerHasPaidBooking(Long customerId, Long bookingId, Long carId, int start, int end) {
        Booking booking = booking(bookingId, customerId, carId, start, end, BookingStatus.APPROVED);
        booking.setPaid(true);
        bookings.put(bookingId, booking);
    }

    @When("customer {long} books car {long} from {int} to {int} days from now")
    public void customerBooksCar(Long customerId, Long carId, int start, int end) {
        BookingRequest request = new BookingRequest(carId, LocalDate.now().plusDays(start), LocalDate.now().plusDays(end));
        run(() -> lastResponse = bookingService.createBooking(request, as(customerId), AUTH_HEADER));
    }

    @When("agency {long} approves booking {long}")
    public void agencyApproves(Long agencyId, Long bookingId) {
        run(() -> lastResponse = bookingService.approveBooking(bookingId, as(agencyId)));
    }

    @When("agency {long} rejects booking {long}")
    public void agencyRejects(Long agencyId, Long bookingId) {
        run(() -> lastResponse = bookingService.rejectBooking(bookingId, as(agencyId)));
    }

    @When("customer {long} cancels booking {long}")
    public void customerCancels(Long customerId, Long bookingId) {
        run(() -> lastResponse = bookingService.cancelBooking(bookingId, as(customerId)));
    }

    @When("agency {long} approves bookings {long} and {long} at the same time")
    public void agencyApprovesBothAtOnce(Long agencyId, Long firstId, Long secondId) throws InterruptedException {
        slowReads = true;
        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.submit(() -> run(() -> bookingService.approveBooking(firstId, as(agencyId))));
        executor.submit(() -> run(() -> bookingService.approveBooking(secondId, as(agencyId))));
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "approvals did not finish in time");
    }

    @Then("the booking is {string} with a total price of {int}")
    public void bookingHasStatusAndPrice(String status, int total) {
        assertNotNull(lastResponse, () -> "booking failed: " + lastError);
        assertEquals(BookingStatus.valueOf(status), lastResponse.status());
        assertEquals(0, BigDecimal.valueOf(total).compareTo(lastResponse.totalPrice()));
    }

    @Then("the request is rejected with {string}")
    public void requestIsRejected(String message) {
        assertNotNull(lastError, "expected the request to fail, but it succeeded");
        assertEquals(message, lastError.getMessage());
    }

    @Then("booking {long} is {string}")
    public void bookingHasStatus(Long bookingId, String status) {
        assertEquals(BookingStatus.valueOf(status), bookings.get(bookingId).getStatus());
    }

    @And("booking {long} is refunded through the payment service")
    public void bookingIsRefunded(Long bookingId) {
        verify(paymentClient).refund(bookingId);
        assertTrue(bookings.get(bookingId).isRefunded());
        assertEquals(false, bookings.get(bookingId).isPaid());
    }

    @And("a {string} event is published")
    public void eventIsPublished(String eventType) {
        verify(bookingEventPublisher).publish(eq(eventType), anyLong(), any(BookingResponse.class), anyString());
    }

    @Then("exactly one of the two bookings is {string}")
    public void exactlyOneIs(String status) {
        long matching = bookings.values().stream()
                .filter(b -> b.getStatus() == BookingStatus.valueOf(status))
                .count();
        assertEquals(1, matching);
    }

    private void run(ThrowingRunnable action) {
        try {
            action.run();
        } catch (Exception e) {
            lastError = e;
        }
    }

    private Authentication as(Long userId) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userId);
        return authentication;
    }

    private Booking booking(Long id, Long renterId, Long carId, int start, int end, BookingStatus status) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setRenterId(renterId);
        booking.setOwnerId(ownerId);
        booking.setCarId(carId);
        booking.setStartDate(LocalDate.now().plusDays(start));
        booking.setEndDate(LocalDate.now().plusDays(end));
        booking.setTotalPrice(pricePerDay.multiply(BigDecimal.valueOf(end - start)));
        booking.setStatus(status);
        return booking;
    }

    private static Booking copy(Booking b) {
        Booking copy = new Booking();
        copy.setId(b.getId());
        copy.setRenterId(b.getRenterId());
        copy.setOwnerId(b.getOwnerId());
        copy.setCarId(b.getCarId());
        copy.setStartDate(b.getStartDate());
        copy.setEndDate(b.getEndDate());
        copy.setStatus(b.getStatus());
        copy.setPaid(b.isPaid());
        copy.setRefunded(b.isRefunded());
        return copy;
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
