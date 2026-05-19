package com.finovara.finovarabackend.expense.exception.notfound;

public class ExpenseNotFoundException extends RuntimeException {
    public ExpenseNotFoundException(String message) {
        super(message);
    }
}
