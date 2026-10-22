package com.ga.RentalSystem.config;

import com.stripe.StripeClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class StripeConfig {

    private StripeClient stripeClient;

    @Bean
    public StripeClient getStripeClient(@Value("${app.stripe.secret.key}") String stripeSecretKey) {
        stripeClient = new StripeClient(stripeSecretKey);
        return stripeClient;
    }
}