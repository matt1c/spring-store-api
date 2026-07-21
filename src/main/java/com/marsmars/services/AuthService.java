package com.marsmars.services;

import com.marsmars.dtos.auth.AuthRequest;
import com.marsmars.dtos.auth.AuthResponse;
import com.marsmars.dtos.auth.RegisterRequest;
import com.marsmars.models.User;
import com.marsmars.repositories.RoleRepository;
import com.marsmars.repositories.UserRepository;
import com.marsmars.security.UserDetailsImpl;
import com.marsmars.util.JwtUtil;
import com.marsmars.util.exceptions.RoleNotFound;
import com.marsmars.util.exceptions.UserEmailAlreadyTaken;
import com.marsmars.util.exceptions.UserNotFound;
import com.marsmars.util.exceptions.UserPasswordIsAlreadyValid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse register(RegisterRequest request) {
        // Проверяем, не занят ли username/email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserEmailAlreadyTaken("Email is already taken");
        }

        // Создаем нового пользователя и обязательно хэшируем пароль!
        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.getRoles().add(roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RoleNotFound("Role not found for registration")));

        userRepository.save(user);

        // Создаем UserDetails оболочку для генерации токена
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        String jwtToken = jwtUtil.generateToken(new HashMap<>(), userDetails);

        return new AuthResponse(jwtToken);
    }

    public AuthResponse authenticate(AuthRequest authRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authRequest.getUsername(),
                        authRequest.getPassword()
                )
        );
        User user = userRepository.findUserByUsername(authRequest.getUsername())
                .orElseThrow(() -> new UserNotFound("User not found"));

        // Генерируем токен
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        String jwtToken = jwtUtil.generateToken(new HashMap<>(), userDetails);

        return new AuthResponse(jwtToken);
    }

    public void updatePassword(AuthRequest authRequest) {
        User user = userRepository.findUserByUsername(authRequest.getUsername())
                .orElseThrow(() -> new UserNotFound("User not found"));

        if(passwordEncoder.matches(authRequest.getPassword(), user.getPassword()))
            throw new UserPasswordIsAlreadyValid("This password is already valid");

        user.setPassword(passwordEncoder.encode(authRequest.getPassword()));

        userRepository.save(user);
    }
}
