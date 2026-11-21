package com.notificationapp.controller;

import com.notificationapp.dto.UserRequest;
import com.notificationapp.dto.UserResponse;
import com.notificationapp.model.User;
import com.notificationapp.service.AuthenticatedUser;
import com.notificationapp.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    // A USER may only reach their own record. An ADMIN may reach every user.
    private static final String ADMIN_OR_OWNER = "hasRole('ADMIN') or #userId == authentication.principal.id";

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest request) {
        UserResponse user = toResponse(userService.create(request));
        return ResponseEntity.created(URI.create("/api/v1/users/" + user.id())).body(user);
    }

    @GetMapping
    public PagedModel<UserResponse> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return new PagedModel<>(userService.list(page, size).map(UserController::toResponse));
    }

    /**
     * The logged-in user. Clients call it after login to learn their id and role.
     */
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return toResponse(userService.get(principal.getId()));
    }

    @PreAuthorize(ADMIN_OR_OWNER)
    @GetMapping("/{userId}")
    public UserResponse get(@PathVariable UUID userId) {
        return toResponse(userService.get(userId));
    }

    @PreAuthorize(ADMIN_OR_OWNER)
    @PutMapping("/{userId}")
    public UserResponse update(@PathVariable UUID userId, @Valid @RequestBody UserRequest request) {
        return toResponse(userService.update(userId, request));
    }

    @PreAuthorize(ADMIN_OR_OWNER)
    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID userId) {
        userService.delete(userId);
    }

    private static UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getRole(), user.getCreatedAt(),
                user.getUpdatedAt());
    }

}
