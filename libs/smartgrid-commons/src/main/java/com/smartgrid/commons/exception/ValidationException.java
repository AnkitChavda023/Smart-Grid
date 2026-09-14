package com.smartgrid.commons.exception;

import java.util.List;

public class ValidationException extends SmartGridException {

    private final List<String> violations;

    public ValidationException(String message, List<String> violations) {
        super(message);
        this.violations = violations;
    }

    public ValidationException(String message) {
        this(message, List.of());
    }

    public List<String> getViolations() {
        return violations;
    }
}
