package com.finovara.corebackend.limit.model;

public enum LimitStatus {
    NONE,    // Brak wydatków
    LOW,     // Niski poziom wykorzystania limitu
    MEDIUM,  // Umiarkowany poziom wykorzystania limitu
    HIGH     // Wysoki poziom wykorzystania / limit osiągnięty
}
