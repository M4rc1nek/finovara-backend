package com.finovara.authbackend.revenue.exception.notfound;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;

/**
 * Deprecated: use {@link com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException} instead.
 */
@Deprecated
public class RevenueNotFoundException extends RequestedEntityNotFoundException {
    public RevenueNotFoundException(String message) {
        super(message);
    }
}
