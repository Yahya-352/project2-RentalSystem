package com.ga.payment_service.controller;

import com.ga.common.dto.response.PageResponse;
import com.ga.payment_service.dto.response.PaymentSummaryResponse;
import com.ga.payment_service.service.PaymentQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Payments", description = "Stripe Checkout payments for approved bookings")
@RestController
@RequestMapping("/pay/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentQueryService paymentQueryService;

    @Operation(summary = "Payment history",
            description = "Admins see all payments and can filter by ownerId (agency), renterId and status. "
                    + "Agencies see payments for their own cars; customers see their own payments.")
    @PreAuthorize("hasAnyRole('ADMIN','AGENCY','CUSTOMER')")
    @GetMapping
    public PageResponse<PaymentSummaryResponse> getPayments(Authentication authentication,
                                                            @RequestParam(required = false) Long ownerId,
                                                            @RequestParam(required = false) Long renterId,
                                                            @RequestParam(required = false) String status,
                                                            @PageableDefault(size = 15, sort = "id",
                                                                    direction = Sort.Direction.DESC) Pageable pageable) {
        return paymentQueryService.getPayments(authentication, ownerId, renterId, status, pageable);
    }
}
