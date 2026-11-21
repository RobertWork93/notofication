package com.notificationapp.notification.service;

/**
 * Turns an exception into the text stored in a {@code last_error} column (at most 1000 characters).
 */
final class FailureMessages {

    private static final int MAX_LENGTH = 1000;

    private FailureMessages() {
    }

    static String of(Throwable failure) {
        String message = failure.getMessage() != null ? failure.getMessage() : failure.getClass().getSimpleName();
        return message.length() <= MAX_LENGTH ? message : message.substring(0, MAX_LENGTH);
    }

}
