package com.soccialy.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

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
     * Handles DTO validation failures triggered by {@code @Valid} and {@code @Validated} annotations.
     * <p>
     * This method extracts all field validation errors from the binding result and returns
     * them as a field-to-message map so the client can display precise form errors.
     * </p>
     * <p>Example JSON response:
     * <pre>
     * {
     *     "error": "Validation failed",
     *     "fields": {
     *         "email": "Please provide a valid email address",
     *         "password": "Password must be at least 8 characters long"
     *     }
     * }
     * </pre>
     * </p>
     *
     * @param ex The {@link MethodArgumentNotValidException} thrown by Spring during validation.
     * @return A {@link ResponseEntity} with a 400 Bad Request status and the validation messages.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex)
    {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors())
        {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "Validation failed",
                        "fields", fieldErrors
                ));
    }
}