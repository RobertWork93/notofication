package com.notificationapp.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.nio.charset.StandardCharsets;

/**
 * Body of user sign-up and update. The password is 8 to 72 characters and at most 72 bytes in UTF-8,
 * the BCrypt input limit.
 */
public record UserRequest(@NotBlank @Email @Size(max = 320) String email,
                          @NotBlank @Size(max = 100) String name,
                          @NotBlank @Size(min = 8, max = 72) String password) {

    /**
     * Trims the email before validation, so surrounding spaces do not make a valid address invalid.
     */
    public UserRequest {
        email = email == null ? null : email.trim();
    }

    /**
     * BCrypt accepts at most 72 bytes. {@code @Size} counts characters, so a password with non-ASCII
     * characters could pass {@code @Size} and still be too long for BCrypt.
     */
    @AssertTrue(message = "must be at most 72 bytes in UTF-8")
    public boolean isPasswordWithinBcryptLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    /**
     * Keeps the password out of logs.
     */
    @Override
    public String toString() {
        return "UserRequest[email=" + email + ", name=" + name + ", password=***]";
    }

}
