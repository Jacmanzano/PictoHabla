package com.cmanzano.pictohabla.common.exception;

import org.springframework.http.HttpStatus;

public class AccountLockedException extends ApiException {
    public AccountLockedException(String message) {
        super(message, HttpStatus.LOCKED);
    }
}
