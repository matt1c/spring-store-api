package com.marsmars.controllers;

import com.marsmars.dtos.user.UserRequest;
import com.marsmars.dtos.user.UserResponse;
import com.marsmars.security.UserDetailsImpl;
import com.marsmars.services.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public UserResponse show(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return profileService.showProfile(userDetails.user().getId());
    }

    @PutMapping
    public ResponseEntity<String> edit(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                       @RequestBody @Valid UserRequest request) {
        profileService.update(request, userDetails.user().getId());
        return ResponseEntity.status(HttpStatus.OK).body("Profile was updated");
    }

    @DeleteMapping
    public ResponseEntity<String> delete(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        profileService.delete(userDetails.user().getId());
        return ResponseEntity.status(HttpStatus.OK).body("Profile was deleted");
    }
}
