package com.notificationapp.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings of the notification workers ({@code app.notifications.*}).
 * The poll intervals ({@code outbox-poll-interval-ms}, {@code delivery-poll-interval-ms}) are read directly
 * by the workers' {@code @Scheduled} annotations, so they are not repeated here.
 *
 * @param batchSize records or jobs a worker processes per run
 */
@ConfigurationProperties("app.notifications")
public record NotificationProperties(@DefaultValue("50") int batchSize) {
}
