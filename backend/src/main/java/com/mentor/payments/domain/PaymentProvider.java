package com.mentor.payments.domain;

/** Port for payment integration; status never executes a transaction. */
public interface PaymentProvider {
    Status status();
    record Status(String provider, boolean testMode, boolean chargesEnabled) { }
}