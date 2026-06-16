package com.finovara.authservice.user.exception.notfound;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;

/**
 * Deprecated: use {@link com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException} instead.
 */
@Deprecated
public class UserNotFoundException extends RequestedEntityNotFoundException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
