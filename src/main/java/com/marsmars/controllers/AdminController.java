package com.marsmars.controllers;

import com.marsmars.dtos.role.RoleRequest;
import com.marsmars.dtos.user.UserResponse;
import com.marsmars.services.AdminService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admins")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public Page<UserResponse> findAll(@RequestParam(name = "page", defaultValue = "0") int pageNum,
                                      @RequestParam(name = "size", defaultValue = "10") int pageSize) {
        return adminService.findAll(pageNum, pageSize);
    }

    @GetMapping("/users/{id}")
    public UserResponse findOne(@PathVariable("id") Long id) {return adminService.findOne(id);}

    @PostMapping("/users/{id}/ban")
    public ResponseEntity<String> userBan(@PathVariable("id") Long id) {
        adminService.banUser(id);
        return ResponseEntity.status(HttpStatus.OK).body("User has been banned");
    }

    @PostMapping("/users/{id}/unban")
    public ResponseEntity<String> userUnban(@PathVariable("id") Long id) {
        adminService.unbanUser(id);
        return ResponseEntity.status(HttpStatus.OK).body("User has been unbanned");
    }

    @PostMapping("/users/{id}/assign-role")
    public ResponseEntity<String> assignRoleToUser(@PathVariable("id") Long id,
                                                   @RequestBody @Valid RoleRequest request) {
        adminService.assignRole(id, request);
        return ResponseEntity.status(HttpStatus.OK).body("Role has been assigned to user");
    }
}
