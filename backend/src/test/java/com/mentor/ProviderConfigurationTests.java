package com.mentor;

import com.mentor.payments.infrastructure.SimulatedPaymentProvider;
import com.mentor.video.infrastructure.DummyVideoProvider;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProviderConfigurationTests {
    @Test void realOrUnknownProviderNamesDoNotSilentlyFallBackToSimulation() {
        for (String provider : new String[]{"STRIPE", "kushki", "", "SIMULATED "}) {
            assertThrows(IllegalArgumentException.class, () -> new SimulatedPaymentProvider(provider));
        }
        for (String provider : new String[]{"webrtc", "ZOOM", "", "DUMMY "}) {
            assertThrows(IllegalArgumentException.class, () -> new DummyVideoProvider(provider));
        }
    }
}