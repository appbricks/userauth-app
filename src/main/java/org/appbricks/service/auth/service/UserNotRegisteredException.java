package org.appbricks.service.auth.service;

/**
 * Exception thrown if user is not registered
 */
public class UserNotRegisteredException
    extends RuntimeException {

    public UserNotRegisteredException(String message, Object... args) {
        super(String.format(message, args));
    }

    public UserNotRegisteredException(Throwable cause, String message, Object... args) {
        super(String.format(message, args), cause);
    }
}
