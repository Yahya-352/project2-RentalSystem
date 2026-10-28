package com.ga.RentalSystem.service;

import com.ga.RentalSystem.dto.response.BookingResponse;
import com.ga.RentalSystem.dto.response.PaymentResponse;
import com.ga.RentalSystem.enums.BookingStatus;
import com.ga.RentalSystem.enums.PaymentStatus;
import com.ga.RentalSystem.exceptions.BadRequestException;
import com.ga.RentalSystem.exceptions.ConflictException;
import com.ga.RentalSystem.exceptions.ForbiddenException;
import com.ga.RentalSystem.exceptions.InformationNotFoundException;
import com.ga.RentalSystem.model.Booking;
import com.ga.RentalSystem.model.Payment;
import com.ga.RentalSystem.model.User;
import com.ga.RentalSystem.repository.BookingRepository;
import com.ga.RentalSystem.repository.PaymentRepository;
import com.ga.RentalSystem.repository.UserRepository;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;

import com.stripe.net.RequestOptions;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.model.checkout.Session;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;


/**
 * Payments through Stripe Checkout.
 * <p>
 * A renter pays for an approved booking on Stripe's hosted page ({@link #checkout}); Stripe then
 * calls the webhook, which marks the payment paid or failed ({@link #capturePayment}). Cancelling
 * a paid booking refunds it ({@link #refund}). Prices are stored in BHD, but the Stripe account
 * cannot take BHD yet, so for now the same number is charged in USD (no conversion).
 */
@Transactional
@Slf4j
@Service
public class StripeService {
    private final StripeClient stripeClient;
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final String frontendUrl;

    public StripeService(StripeClient stripeClient,
                         PaymentRepository paymentRepository,
                         BookingRepository bookingRepository, UserRepository userRepository, NotificationService notificationService, AuditLogService auditLogService,
                         @Value("${app.frontend.url}") String frontendUrl) {
        this.stripeClient = stripeClient;
        this.paymentRepository = paymentRepository;

        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
        this.frontendUrl = frontendUrl;
    }

    /**
     * Creates a Stripe Checkout session for one of the logged-in user's approved bookings and
     * records a PENDING payment for it. The booking row is locked while this runs, so two
     * clicks on "Pay" cannot create two sessions for the same booking.
     *
     * @param bookingId      the id of the booking to pay
     * @param authentication the logged-in user, who must be the renter
     * @return the session id and the URL of Stripe's payment page
     * @throws InformationNotFoundException if the user or booking does not exist
     * @throws ForbiddenException           if the booking belongs to another user
     * @throws BadRequestException          if the booking is not approved
     * @throws ConflictException            if the booking already has a pending or paid payment
     */
    public PaymentResponse checkout(Long bookingId ,
                                    Authentication authentication) {
        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new InformationNotFoundException("User not found"));

        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new InformationNotFoundException("Booking not found"));

        log.info("Checkout started for booking {} by user {}", bookingId, currentUser.getId());

        if (!booking.getRenter().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You are not allowed to perform this action");
        }

        if (!booking.getStatus().equals(BookingStatus.APPROVED)) {
            log.warn("Checkout for booking {} rejected: status is {}", bookingId, booking.getStatus());
            throw new BadRequestException("Only approved Bookings can be paid");
        }

        if (paymentRepository.existsByBookingIdAndStatus(bookingId, PaymentStatus.PENDING)) {
            log.warn("Checkout for booking {} rejected: payment already pending", bookingId);
            throw new ConflictException("Booking " + bookingId + " already has a pending payment");
        }
        if (paymentRepository.existsByBookingIdAndStatus(bookingId, PaymentStatus.PAID)) {
            log.warn("Checkout for booking {} rejected: already paid", bookingId);
            throw new ConflictException("Booking " + bookingId + " is already paid");
        }
        SessionCreateParams.LineItem.PriceData.ProductData productData =
                SessionCreateParams.LineItem.PriceData.ProductData.builder().setName(
                        "Booking " + booking.getId().toString() + " - " + booking.getCar().getModel()
                ).build();

        BigDecimal amount = booking.getTotalPrice().setScale(2, RoundingMode.HALF_UP);

        SessionCreateParams.LineItem.PriceData priceData =
                SessionCreateParams.LineItem.PriceData.builder().setCurrency("usd")
                        .setUnitAmount(amount.movePointRight(2).longValueExact()).setProductData(productData)
                        .build();

        SessionCreateParams.LineItem lineItem = SessionCreateParams.LineItem.builder()
                .setQuantity(1L).setPriceData(priceData).build();

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(frontendUrl + "/payment/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(frontendUrl + "/payment/cancel?bookingId=" + booking.getId())
                .addLineItem(lineItem).build();

        Session session;
        try {
            session = stripeClient.v1().checkout().sessions().create(params);
            log.info("Checkout session {} created for booking {}", session.getId(), bookingId);
        } catch (StripeException e) {
            log.error("Checkout for booking {} failed", bookingId, e);
            throw new RuntimeException(e);
        }

        PaymentResponse paymentResponse = new PaymentResponse(
                session.getStatus() , "SESSION_CREATED" ,
                session.getId() , session.getUrl()
        );

        Payment payment = new Payment();

        payment.setAmount(amount);
        payment.setCurrency("USD");
        payment.setStatus(PaymentStatus.PENDING);
        payment.setSessionId(session.getId());
        payment.setBooking(booking);
        paymentRepository.save(payment);
        return paymentResponse;
    }

    /**
     * Handles a verified Stripe webhook event. Only pending payments are changed, so a
     * repeated event has no effect:
     * <ul>
     *   <li>{@code checkout.session.completed} and paid: the payment becomes PAID, the booking is
     *       marked paid, and the renter and the car owner are notified</li>
     *   <li>{@code checkout.session.expired}: the payment becomes FAILED and the renter is notified</li>
     * </ul>
     * Both are written to the audit log. Other event types are ignored, and an event for an
     * unknown session is only logged.
     *
     * @param event the event, already checked against the webhook signing secret
     * @throws InformationNotFoundException if the event does not contain a checkout session
     */
    public void capturePayment(Event event){
        log.info("capture payment request");
        String type = event.getType();

        if (!type.equals("checkout.session.completed") && !type.equals("checkout.session.expired")) {
            log.info("Ignoring event type {}", type);
            return;
        }

        Object eventObject = event.getDataObjectDeserializer().getObject().orElse(null);

        if(!(eventObject instanceof Session sessionRetrieved)){
            log.error("Event {} does not contain a Session object", event.getId());
            throw new InformationNotFoundException("Could not read Session from event");
        }
        Payment payment = paymentRepository.findBySessionId(sessionRetrieved.getId());


        if(payment!=null){
            Booking booking = payment.getBooking();
            if(payment.getStatus().equals(PaymentStatus.PENDING)){
                if(event.getType().equals("checkout.session.completed")
                        && sessionRetrieved.getPaymentStatus().equals("paid")){
                    payment.setPaymentIntent(sessionRetrieved.getPaymentIntent());
                    payment.setStatus(PaymentStatus.PAID);
                    payment.getBooking().setPaid(true);

                    BookingResponse response = BookingResponse.from(booking);
                    String message = "Payment " + payment.getId() + " for booking " + booking.getId() + " completed";
                    log.info(message);
                    auditLogService.log(booking.getRenter().getId(), "PAYMENT_COMPLETED", "Payment", payment.getId(), message);

                    notificationService.sendEvent(booking.getRenter().getId(), "PAYMENT_RECEIVED", response);
                    notificationService.sendEvent(booking.getCar().getOwner().getId(), "BOOKING_PAID", response);

                }else if (event.getType().equals("checkout.session.expired")){
                    payment.setStatus(PaymentStatus.FAILED);
                    String message = "Payment " + payment.getId() + " for booking " + booking.getId() + " expired";
                    log.info(message);
                    auditLogService.log(booking.getRenter().getId(), "PAYMENT_FAILED", "Payment", payment.getId(), message);
                    notificationService.sendEvent(booking.getRenter().getId(), "PAYMENT_FAILED", BookingResponse.from(booking));
                }
                paymentRepository.save(payment);
            }
        }else{
            log.error("Payment Not Found");
        }
    }

    /**
     * Refunds the full amount of a booking's paid payment through Stripe, then marks the payment
     * REFUNDED and the booking no longer paid, and writes it to the audit log. The request uses
     * the idempotency key {@code refund-booking-{id}}, so if it is sent twice (for example a retry
     * after an error), Stripe refunds only once.
     *
     * @param booking the booking being cancelled
     * @throws InformationNotFoundException if the booking has no paid payment
     * @throws RuntimeException             if Stripe rejects the refund
     */
    public void refund(Booking booking){
        Payment payment = paymentRepository.findByBookingIdAndStatus(booking.getId(), PaymentStatus.PAID)
                .orElseThrow(() -> new InformationNotFoundException("No paid payment for this booking"));
        RefundCreateParams params = RefundCreateParams.builder()
                .setPaymentIntent(payment.getPaymentIntent())
                .build();
        RequestOptions options = RequestOptions.builder()
                .setIdempotencyKey("refund-booking-" + booking.getId())
                .build();
        try {
            stripeClient.v1().refunds().create(params, options);
            booking.setPaid(false);
            payment.setStatus(PaymentStatus.REFUNDED);
            String message = "Payment " + payment.getId() + " for booking " + booking.getId() + " refunded";
            log.info(message);
            auditLogService.log(booking.getRenter().getId(), "PAYMENT_REFUNDED", "Payment", payment.getId(), message);

        } catch (StripeException e) {
            log.error("Refund for booking {} failed", booking.getId(), e);
            throw new RuntimeException(e);
        }
    }
}
