package com.ga.payment_service.service;

import com.ga.common.dto.response.PageResponse;
import com.ga.common.exceptions.BadRequestException;
import com.ga.payment_service.dto.response.PaymentSummaryResponse;
import com.ga.payment_service.enums.PaymentStatus;
import com.ga.payment_service.model.Payment;
import com.ga.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

/**
 * Read-only payment history. Admins see every payment and can filter by agency (owner),
 * renter and status; an agency sees only payments for its own cars; a customer sees only
 * their own payments.
 */
@Service
@RequiredArgsConstructor
public class PaymentQueryService {

    private final PaymentRepository paymentRepository;

    public PageResponse<PaymentSummaryResponse> getPayments(Authentication authentication, Long ownerId,
                                                            Long renterId, String status, Pageable pageable) {
        Long loggedIn = (Long) authentication.getPrincipal();
        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst().orElse("");

        if (role.equals("ROLE_AGENCY")) {
            ownerId = loggedIn;
        } else if (role.equals("ROLE_CUSTOMER")) {
            renterId = loggedIn;
        }

        Specification<Payment> spec = (root, query, cb) -> cb.conjunction();

        if (ownerId != null) {
            Long owner = ownerId;
            spec = spec.and((root, query, cb) -> cb.equal(root.get("ownerId"), owner));
        }
        if (renterId != null) {
            Long renter = renterId;
            spec = spec.and((root, query, cb) -> cb.equal(root.get("renterId"), renter));
        }
        if (status != null && !status.isBlank()) {
            PaymentStatus paymentStatus;
            try {
                paymentStatus = PaymentStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid payment status: " + status);
            }
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), paymentStatus));
        }

        return PageResponse.from(paymentRepository.findAll(spec, pageable).map(PaymentSummaryResponse::from));
    }
}
