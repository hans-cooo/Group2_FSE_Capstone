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
public class AccountCreatedEvent {
    private Long accountId;
    private String accountNumber;
    private String accountType;
    private Long customerId;
    private String currency;
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
