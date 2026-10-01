package com.group2.fse.ledger_service.controller;

import com.group2.fse.ledger_service.dto.TransferRequestDto;
import com.group2.fse.ledger_service.dto.TransferResponseDto;
import com.group2.fse.ledger_service.security.jwt.UserPrincipal;
import com.group2.fse.ledger_service.service.AccountBalanceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for double-entry atomic fund transfers between accounts.
 * Endpoints: /api/v1/ledger/transfers (with singular alias /transfer).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ledger")
public class LedgerTransferController {

    private final AccountBalanceService accountBalanceService;
    private final MeterRegistry meterRegistry;

    @org.springframework.beans.factory.annotation.Autowired
    public LedgerTransferController(AccountBalanceService accountBalanceService, MeterRegistry meterRegistry) {
        this.accountBalanceService = accountBalanceService;
        this.meterRegistry = meterRegistry != null ? meterRegistry : new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
    }

    public LedgerTransferController(AccountBalanceService accountBalanceService) {
        this(accountBalanceService, new io.micrometer.core.instrument.simple.SimpleMeterRegistry());
    }

    @PostMapping({"/transfers", "/transfer"})
    public ResponseEntity<TransferResponseDto> transfer(
            @Valid @RequestBody TransferRequestDto requestDto,
            Authentication authentication,
            HttpServletRequest request) {

        meterRegistry.counter("banking.transfers.attempted.total").increment();
        Long actorId = extractActorId(authentication);
        String clientIp = extractClientIp(request);

        log.info("REST transfer requested: src={}, dst={}, amount={}, ref={}, actorId={}",
                requestDto.getSourceAccountId(), requestDto.getDestinationAccountId(),
                requestDto.getAmount(), requestDto.getReferenceNo(), actorId);

        try {
            TransferResponseDto response = accountBalanceService.executeTransfer(requestDto, actorId, clientIp);
            meterRegistry.counter("banking.transfers.completed.total", "status", "SUCCESS").increment();
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception ex) {
            meterRegistry.counter("banking.transfers.failed.total", "exception", ex.getClass().getSimpleName()).increment();
            throw ex;
        }
    }

    private Long extractActorId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            boolean isStaff = principal.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_TELLER".equals(a.getAuthority()));
            return isStaff ? principal.getUserId() : null;
        }
        return null;
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }
}
