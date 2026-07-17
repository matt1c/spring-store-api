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
        RegisterRequest req = new RegisterRequest("Thomas", "thomas@gmail.com", "123123");
        User user = new User(1L, "Thomas", "123123", "thomas@gmail.com", true);
        Role role = new Role("ROLE_USER");

        Mockito.when(userRepository.existsByEmail("thomas@gmail.com")).thenReturn(false);
        Mockito.when(userRepository.save(any(User.class))).thenReturn(user);
        Mockito.when(passwordEncoder.encode("password")).thenReturn("hashedPassword");
        Mockito.when(roleRepository.findByName("ROLE_USER")).then(Optional.of(role));
        Mockito.when(jwtUtil.generateToken(anyMap(), any(UserDetailsImpl.class)))
                .thenReturn("mocked-jwt-token");


        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        AuthResponse resp = authService.register(req);

        Mockito.verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        Assertions.assertEquals("john@mail.com", savedUser.getEmail());
        Assertions.assertEquals("hashedPassword", savedUser.getPassword());
        Assertions.assertTrue(savedUser.getRoles().contains(role));
        Assertions.assertEquals("mocked-jwt-token", resp.getToken());
    }
}
