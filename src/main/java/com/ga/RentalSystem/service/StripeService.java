package com.ga.RentalSystem.service;

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

import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.model.checkout.Session;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class StripeService {
    private final StripeClient stripeClient;
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final String frontendUrl;

    public StripeService(StripeClient stripeClient,
                         PaymentRepository paymentRepository,
                         BookingRepository bookingRepository, UserRepository userRepository,
                         @Value("${app.frontend.url}") String frontendUrl) {
        this.stripeClient = stripeClient;
        this.paymentRepository = paymentRepository;

        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.frontendUrl = frontendUrl;
    }

    @Transactional
    public PaymentResponse checkout(Long bookingId ,
                                    Authentication authentication) {
        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new InformationNotFoundException("User not found"));

        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new InformationNotFoundException("Booking not found"));

        if (!booking.getRenter().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You are not allowed to perform this action");
        }

        if (!booking.getStatus().equals(BookingStatus.APPROVED)) {
            throw new BadRequestException("Only approved Bookings can be paid");
        }

        if (paymentRepository.existsByBookingIdAndStatus(bookingId, PaymentStatus.PENDING)) {
            throw new ConflictException("Booking " + bookingId + " already has a pending payment");
        }
        if (paymentRepository.existsByBookingIdAndStatus(bookingId, PaymentStatus.PAID)) {
            throw new ConflictException("Booking " + bookingId + " is already paid");
        }
        SessionCreateParams.LineItem.PriceData.ProductData productData =
                SessionCreateParams.LineItem.PriceData.ProductData.builder().setName(
                        "Booking " + booking.getId().toString() + " - " + booking.getCar().getModel()
                ).build();

        SessionCreateParams.LineItem.PriceData priceData =
                SessionCreateParams.LineItem.PriceData.builder().setCurrency("bhd")
                        .setUnitAmount(booking.getTotalPrice().movePointRight(3).longValueExact()).setProductData(productData)
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
        } catch (StripeException e) {
            throw new RuntimeException(e);
        }

        PaymentResponse paymentResponse = new PaymentResponse(
                session.getStatus() , "SESSION_CREATED" ,
                session.getId() , session.getUrl()
        );

        Payment payment = new Payment();

        payment.setAmount(booking.getTotalPrice());
        payment.setCurrency("BHD");
        payment.setStatus(PaymentStatus.PENDING);
        payment.setSessionId(session.getId());
        payment.setBooking(booking);
        paymentRepository.save(payment);
        return paymentResponse;
    }

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
            if(payment.getStatus().equals(PaymentStatus.PENDING)){
                if(event.getType().equals("checkout.session.completed")
                        && sessionRetrieved.getPaymentStatus().equals("paid")){
                    payment.setStatus(PaymentStatus.PAID);

                }else if (event.getType().equals("checkout.session.expired")){
                    payment.setStatus(PaymentStatus.FAILED);
                }
                paymentRepository.save(payment);
            }
        }else{
            log.error("Payment Not Found");
        }

    }
}
