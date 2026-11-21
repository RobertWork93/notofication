package com.notificationapp.app.configuration;

import com.notificationapp.app.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Creates the first admin at startup from {@code app.admin.email} and {@code app.admin.password}
 * (the {@code ADMIN_EMAIL} and {@code ADMIN_PASSWORD} environment variables).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private final UserService userService;

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(adminEmail) || !StringUtils.hasText(adminPassword)) {
            log.warn("ADMIN_EMAIL or ADMIN_PASSWORD is not set, so no admin user is created");
            return;
        }
        userService.createAdminIfMissing(adminEmail, adminPassword).ifPresentOrElse(
                admin -> log.info("Created admin user {}", admin.getEmail()),
                () -> log.warn("Admin user not created: a user with email {} already exists. "
                        + "If that user is not an ADMIN, there is no admin.", adminEmail));
    }

}
