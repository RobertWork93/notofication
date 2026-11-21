package com.notificationapp.web.controller;

import com.notificationapp.configuration.SecurityConfiguration;
import com.notificationapp.controller.UserController;
import com.notificationapp.dto.UserRequest;
import com.notificationapp.service.NotFoundException;
import com.notificationapp.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static com.notificationapp.support.TestDataFactory.USER_ID;
import static com.notificationapp.support.TestDataFactory.jane;
import static com.notificationapp.support.TestDataFactory.user;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfiguration.class)
@WithMockUser(roles = "ADMIN")
class UserControllerTest {

    private static final Instant NOW = Instant.parse("2026-11-21T15:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void shouldReturnCreatedUserWithLocation() throws Exception {
        when(userService.create(new UserRequest("jane.doe@example.com", "Jane Doe", "secret123")))
                .thenReturn(jane(NOW));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "jane.doe@example.com", "name": "Jane Doe", "password": "secret123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/users/" + USER_ID))
                .andExpect(jsonPath("$.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.email").value("jane.doe@example.com"))
                .andExpect(jsonPath("$.name").value("Jane Doe"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.createdAt").value("2026-11-21T15:00:00Z"))
                .andExpect(jsonPath("$.updatedAt").value("2026-11-21T15:00:00Z"));
    }

    @Test
    void shouldRejectInvalidUserBody() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "not-an-email", "name": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
        verify(userService, never()).create(any());
    }

    @Test
    void shouldRejectMalformedJson() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": "))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void shouldRejectNameLongerThan100Characters() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "jane.doe@example.com", "name": "%s", "password": "secret123"}
                                """.formatted("x".repeat(101))))
                .andExpect(status().isBadRequest());
        verify(userService, never()).create(any());
    }

    @Test
    void shouldRejectPasswordShorterThan8Characters() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "jane.doe@example.com", "name": "Jane Doe", "password": "short"}
                                """))
                .andExpect(status().isBadRequest());
        verify(userService, never()).create(any());
    }

    @Test
    void shouldRejectPasswordLongerThan72Bytes() throws Exception {
        // 40 characters, but 80 bytes in UTF-8: over BCrypt's 72-byte input limit.
        String password = "é".repeat(40);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "jane.doe@example.com", "name": "Jane Doe", "password": "%s"}
                                """.formatted(password)))
                .andExpect(status().isBadRequest());
        verify(userService, never()).create(any());
    }

    @Test
    void shouldRejectMissingPassword() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "jane.doe@example.com", "name": "Jane Doe"}
                                """))
                .andExpect(status().isBadRequest());
        verify(userService, never()).create(any());
    }

    @Test
    void shouldListUsersWithDefaultPagingAndPageShape() throws Exception {
        when(userService.list(0, 20)).thenReturn(new PageImpl<>(List.of(jane(NOW)), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("jane.doe@example.com"))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.size").value(20))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.page.totalPages").value(1));
    }

    @Test
    void shouldPassRequestedPagingToService() throws Exception {
        when(userService.list(2, 50)).thenReturn(new PageImpl<>(List.of(), PageRequest.of(2, 50), 0));

        mockMvc.perform(get("/api/v1/users").param("page", "2").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.number").value(2))
                .andExpect(jsonPath("$.page.size").value(50));
    }

    @Test
    void shouldRejectPageSizeAboveMaximum() throws Exception {
        mockMvc.perform(get("/api/v1/users").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        verify(userService, never()).list(anyInt(), anyInt());
    }

    @Test
    void shouldRejectNegativePage() throws Exception {
        mockMvc.perform(get("/api/v1/users").param("page", "-1"))
                .andExpect(status().isBadRequest());
        verify(userService, never()).list(anyInt(), anyInt());
    }

    @Test
    void shouldReturnUserWhenUserExists() throws Exception {
        when(userService.get(USER_ID)).thenReturn(jane(NOW));

        mockMvc.perform(get("/api/v1/users/{userId}", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID.toString()));
    }

    @Test
    void shouldReturnNotFoundProblemWhenUserDoesNotExist() throws Exception {
        when(userService.get(USER_ID)).thenThrow(new NotFoundException("User", USER_ID));

        mockMvc.perform(get("/api/v1/users/{userId}", USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldRejectMalformedUserId() throws Exception {
        mockMvc.perform(get("/api/v1/users/{userId}", "not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnUpdatedUser() throws Exception {
        when(userService.update(USER_ID, new UserRequest("jane.smith@example.com", "Jane Smith", "secret123")))
                .thenReturn(user("jane.smith@example.com", "Jane Smith", NOW));

        mockMvc.perform(put("/api/v1/users/{userId}", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "jane.smith@example.com", "name": "Jane Smith", "password": "secret123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jane Smith"));
    }

    @Test
    void shouldRejectUpdateWithMissingFields() throws Exception {
        mockMvc.perform(put("/api/v1/users/{userId}", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        verify(userService, never()).update(any(), any());
    }

    @Test
    void shouldReturnNoContentWhenDeletingUser() throws Exception {
        mockMvc.perform(delete("/api/v1/users/{userId}", USER_ID))
                .andExpect(status().isNoContent());

        verify(userService).delete(USER_ID);
    }

    @Test
    void shouldReturnNotFoundWhenDeletingMissingUser() throws Exception {
        doThrow(new NotFoundException("User", USER_ID)).when(userService).delete(USER_ID);

        mockMvc.perform(delete("/api/v1/users/{userId}", USER_ID))
                .andExpect(status().isNotFound());
    }

}
