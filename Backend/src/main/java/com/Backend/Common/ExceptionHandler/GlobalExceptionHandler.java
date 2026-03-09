package com.Backend.Common.ExceptionHandler;

import com.Backend.Chat.exception.ChatAccessDeniedException;
import com.Backend.Chat.exception.ChatNotFoundException;
import com.Backend.Chat.exception.MessageNotFoundException;
import com.Backend.Chat.exception.MessageOwnershipException;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.RestClientException;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException ex) {
        logger.error("Entity not found: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse("NOT_FOUND", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ChatNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleChatNotFound(ChatNotFoundException ex) {
        logger.error("Chat not found: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse("CHAT_NOT_FOUND", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MessageNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotFound(MessageNotFoundException ex) {
        logger.error("Message not found: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse("MESSAGE_NOT_FOUND", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ChatAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleChatAccessDenied(ChatAccessDeniedException ex) {
        logger.warn("Chat access denied: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse("ACCESS_DENIED", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MessageOwnershipException.class)
    public ResponseEntity<ErrorResponse> handleMessageOwnership(MessageOwnershipException ex) {
        logger.warn("Message ownership violation: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse("OWNERSHIP_DENIED", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        logger.warn("Validation failed: {}", errors);
        ErrorResponse error = new ErrorResponse("VALIDATION_ERROR", errors);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        logger.error("Invalid argument: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse("BAD_REQUEST", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        logger.error("Unexpected error", ex);
        ErrorResponse error = new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred");
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(RestClientException.class)
    public String handleRestClientException(RestClientException e) {
        logger.error("Error contacting Gateway for token validation: {}", e.getMessage());
        throw new MessageDeliveryException("Unable to validate token with Gateway");
    }
}
