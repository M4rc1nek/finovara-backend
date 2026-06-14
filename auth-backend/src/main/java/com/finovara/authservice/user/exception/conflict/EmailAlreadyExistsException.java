package com.finovara.authservice.user.exception.conflict;

import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;

/**
 * Deprecated: use {@link com.finovara.contracts.exception.conflict.EntityAlreadyExistsException} instead.
 */
@Deprecated
public class EmailAlreadyExistsException extends EntityAlreadyExistsException {
    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}
