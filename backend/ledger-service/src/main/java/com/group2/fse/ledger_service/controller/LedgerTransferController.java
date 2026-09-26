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
@RequiredArgsConstructor
public class LedgerTransferController {

    private final AccountBalanceService accountBalanceService;

    @PostMapping({"/transfers", "/transfer"})
    public ResponseEntity<TransferResponseDto> transfer(
            @Valid @RequestBody TransferRequestDto requestDto,
            Authentication authentication,
            HttpServletRequest request) {

        Long actorId = extractActorId(authentication);
        String clientIp = extractClientIp(request);

        log.info("REST transfer requested: src={}, dst={}, amount={}, ref={}, actorId={}",
                requestDto.getSourceAccountId(), requestDto.getDestinationAccountId(),
                requestDto.getAmount(), requestDto.getReferenceNo(), actorId);

        TransferResponseDto response = accountBalanceService.executeTransfer(requestDto, actorId, clientIp);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private Long extractActorId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getUserId();
        }
        return 1L;
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }
}
