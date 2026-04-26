package com.soccialy.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception thrown when a user authentication or registration attempt fails.
 * <p>
 * This exception is utilized by the {@code AuthService} to signal business logic
 * violations, such as incorrect credentials, duplicate account details,
 * or failed OAuth2 token verification.
 * </p>
 *
 * @author Apetrei Ionuț-Teodor
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class AuthFailedException extends RuntimeException
{
    /**
     * Constructs a new AuthFailedException with a specific error message.
     *
     * @param message The descriptive error message to be returned to the client.
     */
    public AuthFailedException(String message)
    {
        super(message);
    }
}