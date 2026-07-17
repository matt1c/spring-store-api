package com.marsmars.services;

import com.marsmars.dtos.user.UserRequest;
import com.marsmars.dtos.user.UserResponse;
import com.marsmars.models.Role;
import com.marsmars.models.User;
import com.marsmars.repositories.UserRepository;
import com.marsmars.util.exceptions.UserNotFound;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse showProfile(Long id) {
        return toResponse(userRepository.findById(id)
                .orElseThrow(() -> new UserNotFound("User not found")));
    }

    public void update(UserRequest userRequest, Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFound("User not found with this id"));
        user.setEmail(userRequest.getEmail());
        user.setPassword(passwordEncoder.encode(userRequest.getPassword()));
        user.setUsername(userRequest.getUsername());
        userRepository.save(user);
    }

    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFound("User not found"));
        userRepository.delete(user);
    }

    private UserResponse toResponse(User user) {
        UserResponse resp = new UserResponse();
        resp.setId(user.getId());
        resp.setEmail(user.getEmail());
        resp.setUsername(user.getUsername());
        resp.setRoles(user.getRoles().stream().map(Role::getName).toList());
        return resp;
    }

    private User toModel(UserRequest request) {
        User user = new User();
        user.setPassword(request.getPassword());
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        return user;
    }
}
