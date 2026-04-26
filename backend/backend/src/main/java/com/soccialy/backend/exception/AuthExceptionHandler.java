package com.soccialy.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.Optional;

/**
 * Specialized global exception handler for authentication and validation errors.
 * <p>
 * This component intercepts exceptions thrown by any controller within the application.
 * It specifically handles business-level authentication failures and structural
 * validation errors triggered by Jakarta Validation annotations.
 * </p>
 *
 * @author Apetrei Ionuț-Teodor
 */
@RestControllerAdvice
public class AuthExceptionHandler
{
    /**
     * Handles custom authentication failures thrown manually from the service layer.
     *
     * @param e The {@link AuthFailedException} containing the specific failure reason.
     * @return A {@link ResponseEntity} with a 401 Unauthorized status and the error message.
     */
    @ExceptionHandler(AuthFailedException.class)
    public ResponseEntity<Map<String, String>> handleAuthFailedException(AuthFailedException e)
    {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", e.getMessage()));
    }

    /**
     * Handles DTO validation failures triggered by {@code @Valid} annotations.
     * <p>
     * This method extracts the default message from the first field error found
     * in the binding result to provide a concise error response to the client.
     * </p>
     *
     * @param ex The {@link MethodArgumentNotValidException} thrown by Spring during validation.
     * @return A {@link ResponseEntity} with a 400 Bad Request status and the validation message.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex)
    {
        String errorMessage = Optional.ofNullable(ex.getBindingResult().getFieldError())
                .map(FieldError::getDefaultMessage)
                .orElse("Validation failed");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", errorMessage));
    }
}