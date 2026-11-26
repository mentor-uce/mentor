package com.mentor.video.infrastructure;

import java.util.UUID;
import com.mentor.video.domain.VideoProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DummyVideoProvider implements VideoProvider {
    public DummyVideoProvider(@Value("${mentor.video.provider}") String provider) {
        if (!"DUMMY".equals(provider)) {
            throw new IllegalArgumentException("Only the DUMMY video scaffold is implemented");
        }
    }

    @Override public Room createRoom(UUID bookingId) {
        if (bookingId == null) throw new IllegalArgumentException("Booking id is required");
        return new Room("DUMMY", "dummy-" + bookingId, true, false);
    }
}