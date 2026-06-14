package com.finovara.authservice.ratelimit;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum RateLimitMessage {

    TRY_AGAIN_IN_1HOUR("Zbyt wiele żądań. Spróbuj ponownie później");

    private final String label;

    public String label() {
        return label;
    }

}
