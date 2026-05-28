package com.finovara.corebackend.expense.exception.notfound;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;

/**
 * Deprecated: use {@link com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException} instead.
 */
@Deprecated
public class ExpenseNotFoundException extends RequestedEntityNotFoundException {
    public ExpenseNotFoundException(String message) {
        super(message);
    }
}
