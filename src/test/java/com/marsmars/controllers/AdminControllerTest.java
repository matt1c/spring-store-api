package com.marsmars.controllers;

import com.marsmars.dtos.role.RoleRequest;
import com.marsmars.models.Role;
import com.marsmars.models.User;
import com.marsmars.security.UserDetailsImpl;
import com.marsmars.services.AdminService;
import com.marsmars.util.JwtAuthFilter;
import com.marsmars.util.JwtUtil;
import com.marsmars.util.exceptions.RoleNotFound;
import com.marsmars.util.exceptions.UserAlreadyBanOrUnbanned;
import com.marsmars.util.exceptions.UserNotFound;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@EnableMethodSecurity
@WithMockUser(username = "Joe", password = "123123", roles = {"ADMIN"})
public class AdminControllerTest {
    @MockitoBean
    private AdminService adminService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    private UserDetailsImpl userDetails;

    @BeforeEach
    void setUp() throws ServletException, IOException {
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("Joe");
        mockUser.setPassword("123123");
        mockUser.setEmail("joefraizer@gmail.com");
        mockUser.setEnabled(true);

        Role role = new Role("ROLE_USER");
        Role adminRole = new Role("ROLE_ADMIN");
        mockUser.setRoles(Set.of(role, adminRole));

        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());

        this.userDetails = new UserDetailsImpl(mockUser);
    }

    @Test
    void userBan_shouldReturnOk_whenUserUnbanned() throws Exception {
        // Arrange
        doNothing().when(adminService).banUser(userDetails.user().getId());

        // Act && Assert
        mockMvc.perform(post("/api/admins/users/{id}/ban", userDetails.user().getId())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN_VALUE))
                .andExpect(content().string("User has been banned"));

        verify(adminService, times(1)).banUser(userDetails.user().getId());
    }

    @Test
    void userBan_shouldReturnNotFound_whenUserIsNotExisting() throws Exception {
        // Arrange
        doThrow(new UserNotFound("User not found for banning")).when(adminService).banUser(999L);

        // Act && Assert
        mockMvc.perform(post("/api/admins/users/{id}/ban", 999L)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("User not found for banning"));

        verify(adminService, times(1)).banUser(999L);
    }

    @Test
    void userBan_shouldReturnConflict_whenUserBanned() throws Exception {
        // Arrange
        doThrow(new UserAlreadyBanOrUnbanned("User is already banned")).when(adminService).banUser(userDetails.user().getId());

        // Act && Assert
        mockMvc.perform(post("/api/admins/users/{id}/ban", userDetails.user().getId())
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("User is already banned"));

        verify(adminService, times(1)).banUser(userDetails.user().getId());
    }

    @Test
    void userUnban_shouldReturnOk_whenUserBanned() throws Exception {
        // Arrange
        doNothing().when(adminService).unbanUser(userDetails.user().getId());

        // Act && Assert
        mockMvc.perform(post("/api/admins/users/{id}/unban", userDetails.user().getId())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN_VALUE))
                .andExpect(content().string("User has been unbanned"));

        verify(adminService, times(1)).unbanUser(userDetails.user().getId());
    }

    @Test
    void userUnban_shouldReturnConflict_whenUserUnbanned() throws Exception {
        // Arrange
        doThrow(new UserAlreadyBanOrUnbanned("User is already unbanned"))
                .when(adminService).unbanUser(userDetails.user().getId());

        // Act & Assert
        mockMvc.perform(post("/api/admins/users/{id}/unban", userDetails.user().getId())
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("User is already unbanned"));

        verify(adminService, times(1)).unbanUser(userDetails.user().getId());
    }

    @Test
    void userUnban_shouldReturnNotFound_whenUserIsNotExisting() throws Exception {
        // Arrange
        doThrow(new UserNotFound("User not found for banning")).when(adminService).unbanUser(999L);

        // Act && Assert
        mockMvc.perform(post("/api/admins/users/{id}/unban", 999L)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("User not found for banning"));

        verify(adminService, times(1)).unbanUser(999L);
    }

    @Test
    void assignRoleToUser_shouldReturnOk_whenRoleIsAssignedSuccessfully() throws Exception {
        // Arrange
        RoleRequest req = new RoleRequest("ROLE_MANAGER");

        // Act & Assert
        ArgumentCaptor<RoleRequest> roleCaptor = ArgumentCaptor.forClass(RoleRequest.class);
        mockMvc.perform(post("/api/admins/users/{id}/assign-role", userDetails.user().getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN_VALUE))
                .andExpect(content().string("Role has been assigned to user"));

        verify(adminService, times(1)).assignRole(eq(userDetails.user().getId()), roleCaptor.capture());

        RoleRequest roleForAssert = roleCaptor.getValue();
        assertEquals(req.getName(), roleForAssert.getName());
    }

    @Test
    void assignRoleToUser_shouldReturnNotFound_whenRoleNotExist() throws Exception {
        // Arrange
        RoleRequest req = new RoleRequest("ROLE_NON_EXISTENT");
        doThrow(new RoleNotFound("Role not found"))
                .when(adminService).assignRole(userDetails.user().getId(), req);

        // Act & Assert
        mockMvc.perform(post("/api/admins/users/{id}/assign-role", userDetails.user().getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Role not found"));

        verify(adminService, times(1)).assignRole(userDetails.user().getId(), req);
    }

    @Test
    void assignRoleToUser_shouldReturnNotFound_whenUserNotExist() throws Exception {
        // Arrange
        RoleRequest req = new RoleRequest("ROLE_USER");
        doThrow(new UserNotFound("User not found with this id"))
                .when(adminService).assignRole(999L, req);

        // Act & Assert
        mockMvc.perform(post("/api/admins/users/{id}/assign-role", 999L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("User not found with this id"));

        verify(adminService, times(1)).assignRole(999L, req);
    }

    // ACCESS DENIED WAY
    @Test
    @WithMockUser(username = "Joe")
    void userBan_shouldReturnAccessDenied_whenUserIsNotAdmin() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/admins/users/{id}/ban", 1L)
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Access Denied"));

        verifyNoInteractions(adminService);
    }

    @Test
    @WithMockUser(username = "Joe")
    void unbanUser_shouldReturnAccessDenied_whenUserIsNotAdmin() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/admins/users/{id}/unban", 1L)
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Access Denied"));

        verifyNoInteractions(adminService);
    }

    @Test
    @WithMockUser(username = "Joe")
    void assignRoleToUser_shouldReturnAccessDenied_whenUserIsNotAdmin() throws Exception {
        // Arrange
        RoleRequest req = new RoleRequest("ROLE_USER");

        // Act & Assert
        mockMvc.perform(post("/api/admins/users/{id}/assign-role", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Access Denied"));

        verifyNoInteractions(adminService);
    }

    @Test
    void bulkDiscountToUserOrders_shouldReturnOk() throws Exception {
        mockMvc.perform(post("/api/admins/users/{id}/bulk-discount", 1L)
                        .with(csrf())
                        .contentType(MediaType.TEXT_PLAIN_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN_VALUE))
                .andExpect(content().string("Bulk discount has done"));

        verify(adminService, times(1)).bulkDiscountToUserOrders(1L);
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void bulkDiscountToUserOrders_shouldThrowForbidden_whenUserNotAdmin() throws Exception {
        mockMvc.perform(post("/api/admins/users/{id}/bulk-discount", 1L)
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminService);
    }
}
