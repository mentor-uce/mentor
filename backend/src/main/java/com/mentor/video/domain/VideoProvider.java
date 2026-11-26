package com.mentor.video.domain;

import java.util.UUID;

/** Port for a future external video provider. Dummy rooms are not joinable. */
public interface VideoProvider {
    Room createRoom(UUID bookingId);
    record Room(String provider, String roomId, boolean testMode, boolean joinable) { }
}