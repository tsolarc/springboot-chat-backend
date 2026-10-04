package com.Backend.Chat.service.implementation;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

public record SeekCursor(Date at, Long id) {

    public static String encode(Date at, Long id) {
        String value = at.getTime() + ":" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    public static SeekCursor decode(String encoded) {
        try {
            String value = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
            String[] parts = value.split(":", -1);
            if (parts.length != 2) throw new IllegalArgumentException();
            long millis = Long.parseLong(parts[0]);
            long id = Long.parseLong(parts[1]);
            if (millis < 0 || id <= 0) throw new IllegalArgumentException();
            return new SeekCursor(new Date(millis), id);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid cursor");
        }
    }
}
