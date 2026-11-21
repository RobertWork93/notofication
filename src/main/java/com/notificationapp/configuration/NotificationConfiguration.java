package com.notificationapp.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Binds the notification settings. The nested class turns on the scheduled workers,
 * unless {@code app.notifications.scheduling.enabled=false} (the integration tests drive the workers directly).
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(NotificationProperties.class)
public class NotificationConfiguration {

    @Configuration(proxyBeanMethods = false)
    @EnableScheduling
    @ConditionalOnProperty(name = "app.notifications.scheduling.enabled", havingValue = "true", matchIfMissing = true)
    static class SchedulingConfiguration {
    }

}
