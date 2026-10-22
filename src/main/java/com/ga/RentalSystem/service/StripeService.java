package com.ga.RentalSystem.service;

import com.ga.RentalSystem.dto.request.PaymentRequest;

import com.ga.RentalSystem.dto.response.PaymentResponse;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.model.checkout.Session;
import org.springframework.stereotype.Service;


@Service
public class StripeService {
    private final StripeClient stripeClient;

    public StripeService(StripeClient stripeClient) {
        this.stripeClient = stripeClient;
    }

    public PaymentResponse checkout(PaymentRequest paymentRequest) {
        SessionCreateParams.LineItem.PriceData.ProductData productData =
                SessionCreateParams.LineItem.PriceData.ProductData.builder().setName(
                "TEST_PAYMENT"
        ).build();

        SessionCreateParams.LineItem.PriceData priceData =
                 SessionCreateParams.LineItem.PriceData.builder().setCurrency("USD")
                .setUnitAmount(paymentRequest.amount()).setProductData(productData)
                .build();

        SessionCreateParams.LineItem lineItem = SessionCreateParams.LineItem.builder()
                .setQuantity(1L).setPriceData(priceData).build();

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl("http://localhost:8080/success")
                .setCancelUrl("http://localhost:8080/cancel")
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
        return paymentResponse;
    }
}
