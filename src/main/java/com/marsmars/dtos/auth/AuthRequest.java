package com.marsmars.dtos.auth;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthRequest {
    @NotEmpty(message = "Username should be not empty")
    private String username;

    @NotEmpty(message = "Password should be not empty")
    private String password;
}
