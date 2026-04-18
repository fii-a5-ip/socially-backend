package com.socially.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a user authentication attempt fails.
 * Annotated with 401 "Unauthorized" HTTP status.
 *
 * @author Apetrei Ionuț-Teodor
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class AuthFailedException extends RuntimeException
{
    public AuthFailedException(String message)
    {
        super(message);
    }
}