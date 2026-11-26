package com.mentor.payments.presentation;

import com.mentor.payments.application.PaymentStatusService;
import com.mentor.payments.domain.PaymentProvider;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentStatusController {
    private final PaymentStatusService status;

    public PaymentStatusController(PaymentStatusService status) { this.status = status; }

    @GetMapping("/api/v1/payments/status")
    @SecurityRequirement(name = "bearerAuth")
    public PaymentProvider.Status status() { return status.status(); }
}