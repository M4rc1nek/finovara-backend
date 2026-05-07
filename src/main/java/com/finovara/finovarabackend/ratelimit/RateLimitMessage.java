package com.finovara.finovarabackend.ratelimit;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum RateLimitMessage {

    TRY_AGAIN_IN_1HOUR("Zbyt wiele żądań. Spróbuj ponownie za 1 godzinę");

    private final String label;

    public String label() {
        return label;
    }

}
