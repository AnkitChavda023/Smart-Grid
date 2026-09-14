package com.smartgrid.commons.web;

import com.smartgrid.commons.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Split out of {@link GlobalExceptionHandler} because {@code spring-orm} is an optional dependency
 * of smartgrid-commons — services with no JPA (e.g. mcp-server) don't have this class on their
 * classpath at all, and a handler method referencing it would fail bean introspection at startup.
 * Only registered (see WebAutoConfiguration) when spring-orm is actually present.
 */
@RestControllerAdvice
public class OptimisticLockExceptionHandler {

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorResponse> handleOptimisticLock(ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
        return GlobalExceptionHandler.build(HttpStatus.CONFLICT, "The resource was modified concurrently, please retry with the latest version", request);
    }
}
