package com.group2.fse.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResultDto {

    private boolean mfaRequired;
    private String mfaToken;
    private String deliveryChannel;
    private String maskedDestination;

    // Populated if MFA is not required or already verified
    private AuthResponseDto authData;
}
