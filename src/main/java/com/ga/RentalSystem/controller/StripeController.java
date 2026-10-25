package com.ga.RentalSystem.controller;

import com.ga.RentalSystem.dto.request.PaymentRequest;
import com.ga.RentalSystem.dto.response.PaymentResponse;
import com.ga.RentalSystem.service.StripeService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping
    public PaymentResponse checkout(@RequestBody PaymentRequest paymentRequest) {
        return stripeService.checkout(paymentRequest);
    };

    @PostMapping("/webhook")
    public ResponseEntity<Object> capturePayment(@RequestBody String payload ,
                                                 @RequestHeader(("Stripe-Signature")) String header){
        Event event = null;
        try{
            event = Webhook.constructEvent(payload , header ,StripeWebHookSecret);
            stripeService.capturePayment(event);
        } catch (SignatureVerificationException e) {
            throw new RuntimeException("Invalid webhook signature", e);
        }finally {
            return ResponseEntity.noContent().build();
        }
    }

}
