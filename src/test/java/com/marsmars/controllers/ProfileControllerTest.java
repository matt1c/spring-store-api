package com.marsmars.controllers;

import com.marsmars.dtos.user.UserRequest;
import com.marsmars.dtos.user.UserResponse;
import com.marsmars.models.Role;
import com.marsmars.models.User;
import com.marsmars.security.UserDetailsImpl;
import com.marsmars.services.ProfileService;
import com.marsmars.util.JwtAuthFilter;
import com.marsmars.util.JwtUtil;
import com.marsmars.util.exceptions.UserNotFound;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ProfileController.class)
public class ProfileControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfileService profileService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private UserDetailsImpl userDetails;

    @BeforeEach
    void setUp() throws Exception {
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("Joe");
        mockUser.setPassword("123123");
        mockUser.setEmail("joefraizer@gmail.com");
        mockUser.setEnabled(true);

        Role role = new Role("ROLE_USER");
        mockUser.setRoles(List.of(role));

        Mockito.doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtAuthFilter).doFilter(Mockito.any(), Mockito.any(), Mockito.any());

        this.userDetails = new UserDetailsImpl(mockUser);
    }

    // SHOW

    @Test
    void show_shouldReturnResponse_whenUserDoesExist() throws Exception {
        UserResponse resp = new UserResponse(1L, "Bobby", "bobbyfiescher@gmail.com",
                        Collections.singletonList("ROLE_USER"));
        Mockito.when(profileService.showProfile(resp.getId())).thenReturn(resp);

        mockMvc.perform(get("/api/profile")
                .with(user(userDetails))
                .with(csrf())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isNotEmpty());
    }

    @Test
    void show_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        Mockito.doThrow(new UserNotFound("User not found")).when(profileService).showProfile(Mockito.any(Long.class));

        mockMvc.perform(get("/api/profile")
                        .with(csrf())
                        .with(user(userDetails)))
                .andExpect(status().isNotFound());
    }

    // UPDATE

    @Test
    void edit_shouldReturnStatus_andString_whenRequestIsValid() throws Exception {
        UserRequest req = new UserRequest();
        req.setUsername("Thomas");
        req.setEmail("thomas@gmail.com");
        req.setPassword("123123");
        Mockito.doNothing().when(profileService).update(req, 1L);

        mockMvc.perform(put("/api/profile")
                    .with(csrf())
                    .with(user(userDetails))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().string("Profile was updated"));
    }

    @Test
    void edit_shouldReturnStatus_whenRequestIsInvalid() throws Exception {
        UserRequest req = new UserRequest();
        Mockito.doNothing().when(profileService).update(req, 1L);

        mockMvc.perform(put("/api/profile")
                .with(csrf())
                .with(user(userDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void edit_shouldReturnStatus_whenRequestIsNotExist() throws Exception {
        Mockito.doThrow(new UserNotFound("User not found"))
                .when(profileService).delete(userDetails.user().getId());

        mockMvc.perform(put("/api/profile")
                .with(csrf())
                .with(user(userDetails))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // DELETE

    @Test
    void delete_shouldReturnStatus_whenUserExist() throws Exception {
        Mockito.doNothing().when(profileService).delete(1L);

        mockMvc.perform(delete("/api/profile")
                .with(csrf())
                .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(content().string("Profile was deleted"));
    }

    @Test
    void delete_shouldReturnStatus_whenUserNotExist() throws Exception {
        Mockito.doThrow(new UserNotFound("User not found for deleting"))
                .when(profileService).delete(1L);

        mockMvc.perform(delete("/api/profile")
                .with(csrf())
                .with(user(userDetails)))
                .andExpect(status().isNotFound());
    }
}
