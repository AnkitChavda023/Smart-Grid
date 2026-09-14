package com.smartgrid.commons.exception;

public abstract class SmartGridException extends RuntimeException {

    protected SmartGridException(String message) {
        super(message);
    }

    protected SmartGridException(String message, Throwable cause) {
        super(message, cause);
    }
}
