package com.group2.fse.account_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountClosureCompletedEvent {
    private Long closureRequestId;
    private Long accountId;
    private String accountNumber;
    private Long customerId;
    private Long approvedBy;
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
