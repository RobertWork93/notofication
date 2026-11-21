/**
 * Notification channels: how a delivery job is actually sent.
 *
 * <h2>How sending works</h2>
 * <ul>
 *   <li>{@link com.notificationapp.notification.service.DeliveryProcessor} claims a pending delivery job,
 *       builds a {@link com.notificationapp.notification.channel.NotificationMessage}, and calls the
 *       {@link com.notificationapp.notification.channel.NotificationSender} whose {@code channel()} matches the job.</li>
 *   <li>A sender reports failure by throwing. The job is then marked {@code FAILED} with the error; there are no
 *       retries.</li>
 *   <li>Each channel has its own subpackage ({@code email}, {@code slack}), so a real implementation can keep its
 *       client and settings next to its sender. Today both senders only log.</li>
 *   <li>Senders are called while the job's row is locked, so a real sender must use short timeouts.</li>
 * </ul>
 *
 * <h2>Adding a channel</h2>
 * <ol>
 *   <li>Add the value to {@link com.notificationapp.notification.api.Channel}, for example {@code SMS("sms")}.</li>
 *   <li>Add a Liquibase changeset (in the app module) that extends the {@code channel} CHECK constraints on
 *       {@code alerts} and {@code notification_deliveries}.</li>
 *   <li>Add {@code channel/<name>/<Name>NotificationSender} as a {@code @Component} implementing
 *       {@link com.notificationapp.notification.channel.NotificationSender}. It is picked up automatically.</li>
 *   <li>Add the option to {@code CHANNELS} in {@code frontend/src/app/features/alerts/models/alert.model.ts}.</li>
 * </ol>
 */
package com.notificationapp.notification.channel;
