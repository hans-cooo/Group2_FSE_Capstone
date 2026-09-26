package com.group2.fse.auth_service.security.mfa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MfaChallenge implements Serializable {

    private String mfaToken;
    private Long userId;
    private String username;
    private List<String> roles;
    private String userType; // CUSTOMER or STAFF
    private String code;     // 6-digit OTP
    private Instant createdAt;
}
