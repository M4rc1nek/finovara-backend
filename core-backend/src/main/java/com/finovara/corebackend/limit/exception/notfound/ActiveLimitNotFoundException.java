package com.finovara.corebackend.limit.exception.notfound;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;

/**
 * Deprecated: use {@link com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException} instead.
 */
@Deprecated
public class ActiveLimitNotFoundException extends RequestedEntityNotFoundException {
    public ActiveLimitNotFoundException(String message) {
        super(message);
    }
}
