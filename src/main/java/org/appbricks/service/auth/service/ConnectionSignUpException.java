package org.appbricks.service.auth.service;

/**
 * Runtime exception thrown if an error prevents the signup flow.
 */
public class ConnectionSignUpException
    extends RuntimeException {

    public ConnectionSignUpException(String message, Object... args) {
        super(String.format(message, args));
    }

    public ConnectionSignUpException(Throwable cause, String message, Object... args) {
        super(String.format(message, args), cause);
    }
}
