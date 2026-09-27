package com.group2.fse.account_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerProfileResponse {
    private Long customerId;
    private String username;
    private String email;
    private KycDto kyc;
    private String kycStatus;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class KycDto {
        private Long kycId;
        private String firstName;
        private String middleInitial;
        private String lastName;
        private String address;
        private String mobileNumber;
        private String civilStatus;
        private String occupation;
        private String status;
        private LocalDateTime createdAt;
    }
}
