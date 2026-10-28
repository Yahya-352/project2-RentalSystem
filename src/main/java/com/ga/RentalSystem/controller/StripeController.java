package com.ga.RentalSystem.controller;

import com.ga.RentalSystem.dto.response.PaymentResponse;
import com.ga.RentalSystem.exceptions.BadRequestException;
import com.ga.RentalSystem.service.StripeService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Payments", description = "Stripe Checkout payments for approved bookings")
@RestController
@RequestMapping("pay")
public class StripeController {

    private final StripeService stripeService;
    private final String StripeWebHookSecret;

    public StripeController(StripeService stripeService,
                            @Value("${app.stripe.webhook.secret}") String stripeWebHookSecret) {
        this.stripeService = stripeService;
        StripeWebHookSecret = stripeWebHookSecret;
    }

    @Operation(summary = "Start paying for an approved booking (the renter only)",
            description = "Creates a Stripe Checkout session and returns its URL. The browser is sent there, "
                    + "and Stripe redirects back to the frontend's /payment/success or /payment/cancel page. "
                    + "409 if the booking already has a pending or completed payment.")
    @PostMapping("/{bookingId}")
    public PaymentResponse checkout(@PathVariable Long bookingId ,
                                    Authentication authentication) {
        return stripeService.checkout(bookingId ,authentication);
    };

    @Operation(summary = "Stripe webhook (called by Stripe, not by clients)",
            description = "Verifies the Stripe-Signature header, then marks the payment PAID "
                    + "(checkout.session.completed) or FAILED (checkout.session.expired).")
    @PostMapping("/webhook")
    public ResponseEntity<Object> capturePayment(@RequestBody String payload ,
                                                 @RequestHeader(("Stripe-Signature")) String header){
        Event event = null;
        try{
            event = Webhook.constructEvent(payload , header , StripeWebHookSecret);
            stripeService.capturePayment(event);
            return ResponseEntity.noContent().build();
        } catch (SignatureVerificationException e) {
            throw new BadRequestException("Invalid webhook signature");
        }
    }

}
