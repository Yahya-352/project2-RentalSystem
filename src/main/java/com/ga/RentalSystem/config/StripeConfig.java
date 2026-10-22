package com.ga.RentalSystem.config;

import com.stripe.StripeClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;


@Configuration
public class StripeConfig {

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    @PostConstruct
    public void setStripeSecretKey() {
        Stripe.apiKey = stripeSecretKey;
    }
}