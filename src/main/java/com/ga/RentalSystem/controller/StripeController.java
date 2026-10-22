package com.ga.RentalSystem.controller;

import com.ga.RentalSystem.dto.request.PaymentRequest;
import com.ga.RentalSystem.dto.response.PaymentResponse;
import com.ga.RentalSystem.service.StripeService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("pay")
public class StripeController {
    private final StripeService stripeService;
    public StripeController(StripeService stripeService) {
        this.stripeService = stripeService;
    }

    @PostMapping
    public PaymentResponse checkout(@RequestBody PaymentRequest paymentRequest) {
        return stripeService.checkout(paymentRequest);
    };

}
