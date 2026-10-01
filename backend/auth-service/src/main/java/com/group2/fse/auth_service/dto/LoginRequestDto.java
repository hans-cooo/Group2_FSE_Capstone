package com.group2.fse.auth_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDto {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    /**
     * Optional preferred delivery channel: "SMS", "EMAIL", or "TOTP".
     * If omitted, defaults to SMS for customers and EMAIL for bank staff.
     */
    private String preferredChannel;
}
