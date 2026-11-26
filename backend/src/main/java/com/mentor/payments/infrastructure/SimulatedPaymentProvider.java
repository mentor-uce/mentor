package com.mentor.payments.infrastructure;

import com.mentor.payments.domain.PaymentProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Development scaffold only: no charge, refund or approval operation exists. */
@Component
public class SimulatedPaymentProvider implements PaymentProvider {
    public SimulatedPaymentProvider(@Value("${mentor.payments.provider}") String provider) {
        if (!"SIMULATED".equals(provider)) {
            throw new IllegalArgumentException("Only the SIMULATED payment scaffold is implemented");
        }
    }

    @Override public Status status() { return new Status("SIMULATED", true, false); }
}