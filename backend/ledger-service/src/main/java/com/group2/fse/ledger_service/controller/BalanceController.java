package com.group2.fse.ledger_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group2.fse.ledger_service.dto.BalanceResponseDto;
import com.group2.fse.ledger_service.service.AccountBalanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * High-speed Balance Query Controller with Redis caching.
 * Endpoints: /api/v1/ledger/accounts/{accountId}/balance (with alias /balance/{accountId}).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ledger")
@RequiredArgsConstructor
public class BalanceController {

    private static final String CACHE_KEY_PREFIX = "ledger:balance:";
    private static final Duration CACHE_TTL = Duration.ofSeconds(10);

    private final AccountBalanceService accountBalanceService;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Autowired(required = false)
    private ObjectMapper objectMapper;

    @GetMapping({"/accounts/{accountId}/balance", "/balance/{accountId}"})
    public ResponseEntity<BalanceResponseDto> getBalance(@PathVariable("accountId") Long accountId) {
        String cacheKey = CACHE_KEY_PREFIX + accountId;

        if (redisTemplate != null && objectMapper != null) {
            try {
                String cachedJson = redisTemplate.opsForValue().get(cacheKey);
                if (cachedJson != null && !cachedJson.isBlank()) {
                    BalanceResponseDto cachedDto = objectMapper.readValue(cachedJson, BalanceResponseDto.class);
                    log.debug("Cache hit for account {} balance", accountId);
                    return ResponseEntity.ok(
                            BalanceResponseDto.builder()
                                    .accountId(cachedDto.getAccountId())
                                    .currency(cachedDto.getCurrency())
                                    .availableBalance(cachedDto.getAvailableBalance())
                                    .asOfTimestamp(cachedDto.getAsOfTimestamp())
                                    .isCached(true)
                                    .build()
                    );
                }
            } catch (Exception e) {
                log.warn("Redis balance cache read error for account {}: {}", accountId, e.getMessage());
            }
        }

        BalanceResponseDto authoritativeDto = accountBalanceService.getAccountBalance(accountId);

        if (redisTemplate != null && objectMapper != null) {
            try {
                String json = objectMapper.writeValueAsString(authoritativeDto);
                redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL);
            } catch (Exception e) {
                log.warn("Redis balance cache write error for account {}: {}", accountId, e.getMessage());
            }
        }

        return ResponseEntity.ok(authoritativeDto);
    }
}
