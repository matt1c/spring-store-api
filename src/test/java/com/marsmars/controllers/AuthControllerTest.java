package com.marsmars.controllers;

import com.marsmars.dtos.auth.AuthRequest;
import com.marsmars.dtos.auth.AuthResponse;
import com.marsmars.dtos.auth.RegisterRequest;
import com.marsmars.models.Role;
import com.marsmars.models.User;
import com.marsmars.security.UserDetailsImpl;
import com.marsmars.services.AuthService;
import com.marsmars.util.JwtAuthFilter;
import com.marsmars.util.JwtUtil;
import com.marsmars.util.exceptions.UserEmailAlreadyTaken;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
public class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

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

    // LOGIN

    @Test
    void login_shouldReturnStatus_whenRequestIsValid_andUserExist() throws Exception {
        AuthRequest req = new AuthRequest("Joe", "123123");
        AuthResponse resp = new AuthResponse("some-test-jwt-token");

        Mockito.when(authService.authenticate(req)).thenReturn(resp);

        mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .with(user(userDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void login_shouldReturnStatus_whenRequestIsValid_andUserNotExist() throws Exception {
        AuthRequest req = new AuthRequest("Joe", "123123");
        Mockito.doThrow(new UserNotFound("User not found"))
                .when(authService).authenticate(req);

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void login_shouldReturnStatus_whenRequestIsInvalid() throws Exception {
        AuthRequest req = new AuthRequest("", "123123");
        Mockito.doThrow(new UserNotFound("User not found"))
                .when(authService).authenticate(req);

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // REGISTER

    @Test
    void register_shouldReturnStatus_whenRequestIsValid() throws Exception {
        RegisterRequest req = new RegisterRequest("Joe", "joefraizer@gmail.com", "123123");
        AuthResponse resp = new AuthResponse("some-test-jwt-token");

        Mockito.when(authService.register(req)).thenReturn(resp);

        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                        .with(user(userDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void register_shouldReturnStatus_whenRequestIsInvalid() throws Exception {
        RegisterRequest req = new RegisterRequest("", "joefraizer@gmail.com", "");
        AuthResponse resp = new AuthResponse("some-test-jwt-token");

        Mockito.when(authService.register(req)).thenReturn(resp);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_shouldReturnStatus_whenRequestIsValid_userEmailAlreadyExist() throws Exception {
        RegisterRequest req = new RegisterRequest("Joe", "joefraizer@gmail.com", "123123");

        Mockito.doThrow(new UserEmailAlreadyTaken("User with this email already exists"))
                        .when(authService).register(req);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // CHANGING PASSWORD

    @Test
    void changePassword_ValidRequest_ReturnsOk() throws Exception {
        AuthRequest request = new AuthRequest("john doe", "securePassword123");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/change-password")
                        .with(csrf())
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().string("Password was updated"));

        Mockito.verify(authService).updatePassword(Mockito.any(AuthRequest.class));
    }

    @Test
    void changePassword_InvalidRequest_ReturnsBadRequest() throws Exception {
        AuthRequest invalidRequest = new AuthRequest("john_doe", "");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/change-password")
                        .with(csrf())
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());

        Mockito.verify(authService, Mockito.never()).updatePassword(Mockito.any(AuthRequest.class));
    }
}
