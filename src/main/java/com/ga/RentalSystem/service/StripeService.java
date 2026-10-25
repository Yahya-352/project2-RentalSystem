package com.ga.RentalSystem.service;

import com.ga.RentalSystem.dto.request.PaymentRequest;

import com.ga.RentalSystem.dto.response.PaymentResponse;
import com.ga.RentalSystem.enums.PaymentStatus;
import com.ga.RentalSystem.model.Payment;
import com.ga.RentalSystem.repository.PaymentRepository;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;

import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.model.checkout.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class StripeService {
    private final StripeClient stripeClient;
    private final PaymentRepository paymentRepository;

    public StripeService(StripeClient stripeClient, PaymentRepository paymentRepository) {
        this.stripeClient = stripeClient;
        this.paymentRepository = paymentRepository;
    }

    public PaymentResponse checkout(PaymentRequest paymentRequest) {
        SessionCreateParams.LineItem.PriceData.ProductData productData =
                SessionCreateParams.LineItem.PriceData.ProductData.builder().setName(
                "TEST_PAYMENT"
        ).build();

        SessionCreateParams.LineItem.PriceData priceData =
                 SessionCreateParams.LineItem.PriceData.builder().setCurrency(paymentRequest.currency())
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
        paymentRepository.save(payment);
        return paymentResponse;
    }

    public void capturePayment(Event event){
        log.info("capture payment request");
        Session sessionRetrieved
                = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
        if(sessionRetrieved == null){
            log.error("Session object is null");
            return;
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
