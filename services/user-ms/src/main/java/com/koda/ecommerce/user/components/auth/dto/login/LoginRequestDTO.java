package com.koda.ecommerce.user.components.auth.dto.login;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequestDTO {

    @NotBlank(message = "Email or mobile is required")
    private String identifier;

    @NotBlank(message = "Password is required")
    private String password;
}