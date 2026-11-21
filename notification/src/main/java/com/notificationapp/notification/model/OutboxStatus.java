package com.notificationapp.notification.model;

/**
 * Lifecycle of an outbox record. There are no retries: a record that fails to process is FAILED.
 */
public enum OutboxStatus {

    PENDING,
    PROCESSED,
    FAILED

}
