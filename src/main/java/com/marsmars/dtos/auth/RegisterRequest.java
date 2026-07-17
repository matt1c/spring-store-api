package com.marsmars.dtos.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    @NotEmpty(message = "Username should be not empty")
    private String username;

    @NotEmpty(message = "Email should be not empty")
    @Email(message = "This field should be in email format")
    private String email;

    @NotEmpty(message = "Password should be not empty")
    private String password;
}
