package com.smartgrid.authservice.service;

import java.time.Duration;

public class AccountLockedException extends RuntimeException {

    private final Duration retryAfter;

    public AccountLockedException(Duration retryAfter) {
        super("Too many failed login attempts, try again in " + retryAfter.getSeconds() + "s");
        this.retryAfter = retryAfter;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }
}
