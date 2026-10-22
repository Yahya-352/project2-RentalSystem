package com.ga.RentalSystem.service;

import com.ga.RentalSystem.dto.request.PaymentRequest;

import com.ga.RentalSystem.dto.response.PaymentResponse;
import com.ga.RentalSystem.enums.PaymentStatus;
import com.ga.RentalSystem.model.Payment;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
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

        Payment payment = new Payment();

        payment.setAmount(paymentRequest.amount());
        payment.setCurrency(paymentRequest.currency());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setSessionId(session.getId());

        return paymentResponse;
    }

    public void capturePayment(Event event){
        if(event.getType().equals("checkout.session.completed")){

        }else{

        }

    }
}
