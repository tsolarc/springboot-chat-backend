package com.Backend.Common.ExceptionHandler;

import com.Backend.Chat.exception.*;
import com.Backend.Common.Http.ApiProblems;
import com.Backend.Common.Http.RequestIdFilter;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import java.util.NoSuchElementException;
import java.util.TreeMap;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({EntityNotFoundException.class, ChatNotFoundException.class,
            MessageNotFoundException.class, NoSuchElementException.class})
    ProblemDetail notFound(Exception error, HttpServletRequest request) {
        return ApiProblems.create(HttpStatus.NOT_FOUND, "NOT_FOUND", "The requested resource was not found", request);
    }

    @ExceptionHandler({ChatAccessDeniedException.class, MessageOwnershipException.class, AccessDeniedException.class})
    ProblemDetail forbidden(Exception error, HttpServletRequest request) {
        return ApiProblems.create(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Access denied", request);
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail unauthorized(AuthenticationException error, HttpServletRequest request) {
        return ApiProblems.create(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Invalid credentials", request);
    }

    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
    ProblemDetail badRequest(Exception error, HttpServletRequest request) {
        return ApiProblems.create(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Invalid request parameters", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail conflict(DataIntegrityViolationException error, HttpServletRequest request) {
        return ApiProblems.create(HttpStatus.CONFLICT, "CONFLICT", "The request conflicts with an existing resource", request);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception error, HttpServletRequest request) {
        log.error("Unexpected request failure", error);
        return ApiProblems.create(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException error, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "Request validation failed");
        var fields = new TreeMap<String, String>();
        error.getBindingResult().getFieldErrors().forEach(field -> fields.put(field.getField(), field.getDefaultMessage()));
        problem.setProperty("errors", fields);
        problem.setProperty("code", "VALIDATION_ERROR");
        return handleExceptionInternal(error, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (body instanceof ProblemDetail problem) {
            problem.setProperty("requestId", request.getAttribute(RequestIdFilter.ATTRIBUTE, WebRequest.SCOPE_REQUEST));
            problem.setProperty("message", problem.getDetail());
            if (problem.getProperties() == null || !problem.getProperties().containsKey("code")) {
                problem.setProperty("code", "HTTP_" + status.value());
            }
        }
        return super.createResponseEntity(body, headers, status, request);
    }
}
