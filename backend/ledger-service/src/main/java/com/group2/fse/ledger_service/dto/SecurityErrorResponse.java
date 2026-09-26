package com.group2.fse.ledger_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Task: FSE-405
 * Assigned to: Alyssa
 *
 * Dedicated Security Error Response DTO fulfilling Epic D Parallel Execution Plan specification.
 * Combines RFC-7807 problem details specification with legacy banking error attributes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SecurityErrorResponse {

    // RFC-7807 Problem Details Standard Fields
    private String type;
    private String title;
    private int status;
    private String detail;
    private String instance;
    private String errorCode;
    private String timestamp;
    private List<Map<String, String>> invalidParams;

    // Banking Legacy Fields (for backwards compatibility with earlier client contracts)
    private String error;
    private String message;
    private String path;

    public static SecurityErrorResponse of(
            String type,
            String title,
            int status,
            String detail,
            String instance,
            String errorCode,
            String errorName) {
        String now = Instant.now().toString();
        return SecurityErrorResponse.builder()
                .type(type)
                .title(title)
                .status(status)
                .detail(detail)
                .instance(instance)
                .errorCode(errorCode)
                .timestamp(now)
                .error(errorName)
                .message(detail)
                .path(instance)
                .build();
    }
}
