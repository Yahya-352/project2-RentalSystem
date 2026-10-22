package com.ga.RentalSystem.controller;

import com.ga.RentalSystem.dto.request.PaymentRequest;
import com.ga.RentalSystem.dto.response.PaymentResponse;
import com.ga.RentalSystem.service.StripeService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.net.ApiResource;
import com.stripe.net.Webhook;
import jdk.jfr.Event;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("pay")
public class StripeController {
    private final StripeService stripeService;
    private final String StripeWebHookSecret;
    public StripeController(StripeService stripeService, String stripeWebHookSecret) {
        this.stripeService = stripeService;
        StripeWebHookSecret = stripeWebHookSecret;
    }

    @PostMapping
    public PaymentResponse checkout(@RequestBody PaymentRequest paymentRequest) {
        return stripeService.checkout(paymentRequest);
    };

    @PostMapping("/webhook")
    public void capturePayment(@RequestBody String payload , @RequestHeader String header){
        try{
            Event event = Webhook.constructEvent(payload , header ,"");
        } catch (SignatureVerificationException e) {
            throw new RuntimeException(e);
        }
    }

}
