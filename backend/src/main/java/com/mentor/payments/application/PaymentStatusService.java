package com.mentor.payments.application;

import com.mentor.payments.domain.PaymentProvider;
import org.springframework.stereotype.Service;

@Service
public class PaymentStatusService {
    private final PaymentProvider provider;

    public PaymentStatusService(PaymentProvider provider) { this.provider = provider; }

    public PaymentProvider.Status status() { return provider.status(); }
}