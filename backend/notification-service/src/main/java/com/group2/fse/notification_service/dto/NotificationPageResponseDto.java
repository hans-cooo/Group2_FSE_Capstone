package com.group2.fse.notification_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Task: FSE-601
 * Paginated container for notification feeds.
 * Conforms strictly to API_DESIGN_SPECIFICATION.md Section 6.1.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPageResponseDto {

    private List<NotificationResponseDto> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    @com.fasterxml.jackson.annotation.JsonProperty("isLast")
    private boolean isLast;

    public static NotificationPageResponseDto fromPage(Page<NotificationResponseDto> page) {
        return NotificationPageResponseDto.builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isLast(page.isLast())
                .build();
    }
}
