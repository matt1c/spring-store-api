package com.marsmars.services;

import com.marsmars.dtos.auth.AuthRequest;
import com.marsmars.dtos.auth.AuthResponse;
import com.marsmars.dtos.auth.RegisterRequest;
import com.marsmars.models.Role;
import com.marsmars.models.User;
import com.marsmars.repositories.RoleRepository;
import com.marsmars.repositories.UserRepository;
import com.marsmars.security.UserDetailsImpl;
import com.marsmars.util.JwtUtil;
import com.marsmars.util.exceptions.RoleNotFound;
import com.marsmars.util.exceptions.UserEmailAlreadyTaken;
import com.marsmars.util.exceptions.UserNotFound;
import com.marsmars.util.exceptions.UserPasswordIsAlreadyValid;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private AuthenticationManager authenticationManager;

    @Test
    void authenticate_shouldReturnToken_withCredentialsValid() {
        AuthRequest req = new AuthRequest("Thomas", "123123");
        User user = new User(1L, "Thomas", "123123", "thomas@gmail.com", true);

        Mockito.when(userRepository.findUserByUsername(user.getUsername()))
                .thenReturn(Optional.of(user));
        Mockito.when(jwtUtil.generateToken(anyMap(), any(UserDetailsImpl.class)))
                .thenReturn("mocked-jwt-token");

        AuthResponse resp = authService.authenticate(req);

        Assertions.assertEquals("mocked-jwt-token", resp.getToken());
        Mockito.verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("Thomas", "123123"));
    }

    @Test
    void authenticate_shouldThrowException_withBadCredentials() {
        AuthRequest req = new AuthRequest();

        Mockito.doThrow(new BadCredentialsException("Invalid"))
                .when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        Assertions.assertThrows(BadCredentialsException.class, () -> authService.authenticate(req));
    }

    @Test
    void register_shouldReturnToken_withCredentialsValid() {
        RegisterRequest request = new RegisterRequest("john", "john@mail.com", "password");
        Role userRole = new Role("ROLE_USER");

        Mockito.when(userRepository.existsByEmail("john@mail.com")).thenReturn(false);
        Mockito.when(passwordEncoder.encode("password")).thenReturn("hashedPassword");
        Mockito.when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        Mockito.when(jwtUtil.generateToken(anyMap(), any(UserDetailsImpl.class)))
                .thenReturn("mocked-jwt-token");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        AuthResponse response = authService.register(request);

        Mockito.verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        Assertions.assertEquals("john@mail.com", savedUser.getEmail());
        Assertions.assertEquals("hashedPassword", savedUser.getPassword());
        Assertions.assertTrue(savedUser.getRoles().contains(userRole));
        Assertions.assertEquals("mocked-jwt-token", response.getToken());
    }

    @Test
    void register_shouldThrowUserEmailAlreadyTaken_whenEmailExists() {
        RegisterRequest request = new RegisterRequest("john", "john@mail.com", "password");

        Mockito.when(userRepository.existsByEmail("john@mail.com")).thenReturn(true);

        Assertions.assertThrows(UserEmailAlreadyTaken.class, () -> authService.register(request));
        Mockito.verify(userRepository, Mockito.never()).save(any());
    }

    @Test
    void register_shouldThrowRoleNotFound_whenRoleMissing() {
        RegisterRequest request = new RegisterRequest("john", "john@mail.com", "password");

        Mockito.when(userRepository.existsByEmail("john@mail.com")).thenReturn(false);
        Mockito.when(passwordEncoder.encode("password")).thenReturn("hashedPassword");
        Mockito.when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.empty());

        Assertions.assertThrows(RoleNotFound.class, () -> authService.register(request));
        Mockito.verify(userRepository, Mockito.never()).save(any());
    }

    @Test
    void updatePassword_Success_ShouldUpdateAndSave() {
        AuthRequest request = new AuthRequest("john_doe", "new_password123");
        User existingUser = new User(1L, "john_doe", "old_encoded_password", "john@example.com", true);

        Mockito.when(userRepository.findUserByUsername("john_doe")).thenReturn(Optional.of(existingUser));
        Mockito.when(passwordEncoder.matches("new_password123", "old_encoded_password")).thenReturn(false);
        Mockito.when(passwordEncoder.encode("new_password123")).thenReturn("new_encoded_password");

        authService.updatePassword(request);

        Mockito.verify(userRepository).save(existingUser);
        Assertions.assertEquals("new_encoded_password", existingUser.getPassword());
    }

    @Test
    void updatePassword_UserNotFound_ShouldThrowException() {
        AuthRequest request = new AuthRequest("unknown_user", "password");
        Mockito.when(userRepository.findUserByUsername("unknown_user")).thenReturn(Optional.empty());

        Assertions.assertThrows(UserNotFound.class, () -> authService.updatePassword(request));
        Mockito.verify(userRepository, Mockito.never()).save(Mockito.any(User.class));
    }

    @Test
    void updatePassword_PasswordAlreadySame_ShouldThrowException() {
        AuthRequest request = new AuthRequest("john_doe", "same_password");
        User existingUser = new User(1L, "john_doe", "encoded_same_password", "john@example.com", true);

        Mockito.when(userRepository.findUserByUsername("john_doe")).thenReturn(Optional.of(existingUser));
        Mockito.when(passwordEncoder.matches("same_password", "encoded_same_password")).thenReturn(true);

        Assertions.assertThrows(UserPasswordIsAlreadyValid.class, () -> authService.updatePassword(request));
        Mockito.verify(userRepository, Mockito.never()).save(Mockito.any(User.class));
    }
}
