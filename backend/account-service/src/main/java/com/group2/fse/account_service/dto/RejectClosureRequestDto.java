package com.group2.fse.account_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectClosureRequestDto {

    @NotBlank(message = "Rejection reason is required")
    private String rejectionReason;
}
