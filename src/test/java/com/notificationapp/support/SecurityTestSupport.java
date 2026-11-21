package com.notificationapp.support;

import com.notificationapp.model.User;
import com.notificationapp.service.AuthenticatedUser;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

/**
 * Runs a MockMvc request as an authenticated user, with the same principal type the application uses.
 */
public final class SecurityTestSupport {

    private SecurityTestSupport() {
    }

    public static RequestPostProcessor as(User user) {
        return user(AuthenticatedUser.from(user));
    }

}
