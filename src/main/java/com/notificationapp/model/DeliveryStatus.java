package com.notificationapp.model;

/**
 * Lifecycle of a notification delivery job. There are no retries: a job that fails to send is FAILED.
 */
public enum DeliveryStatus {

    PENDING,
    SENT,
    FAILED

}
