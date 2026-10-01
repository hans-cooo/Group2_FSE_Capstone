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
public class MfaVerifyRequestDto {

    @NotBlank(message = "MFA token is required")
    private String mfaToken;

    @NotBlank(message = "6-digit OTP code is required")
    private String code;
}
