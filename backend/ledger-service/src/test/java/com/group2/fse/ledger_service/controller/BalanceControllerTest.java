package com.group2.fse.ledger_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group2.fse.ledger_service.config.JacksonConfig;
import com.group2.fse.ledger_service.dto.BalanceResponseDto;
import com.group2.fse.ledger_service.exception.AccountNotFoundException;
import com.group2.fse.ledger_service.exception.GlobalExceptionHandler;
import com.group2.fse.ledger_service.service.AccountBalanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Balance Controller Unit Tests (FSE-403)")
class BalanceControllerTest {

    @Mock
    private AccountBalanceService accountBalanceService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private BalanceController controller;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new JacksonConfig().objectMapper();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(controller, "objectMapper", objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/ledger/accounts/{id}/balance: Returns 200 OK from authoritative service on cache miss")
    void shouldReturnAuthoritativeBalanceOnCacheMiss() throws Exception {
        Long accountId = 10L;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("ledger:balance:" + accountId)).thenReturn(null);

        BalanceResponseDto authoritativeDto = BalanceResponseDto.builder()
                .accountId(accountId)
                .currency("PHP")
                .availableBalance(new BigDecimal("5000.0000"))
                .asOfTimestamp(Instant.now())
                .isCached(false)
                .build();

        when(accountBalanceService.getAccountBalance(accountId)).thenReturn(authoritativeDto);

        mockMvc.perform(get("/api/v1/ledger/accounts/{accountId}/balance", accountId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(10))
                .andExpect(jsonPath("$.currency").value("PHP"))
                .andExpect(jsonPath("$.availableBalance").value(5000.0000))
                .andExpect(jsonPath("$.isCached").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/ledger/accounts/{id}/balance: Returns 200 OK with isCached=true on cache hit")
    void shouldReturnCachedBalanceOnCacheHit() throws Exception {
        Long accountId = 10L;
        BalanceResponseDto cachedDto = BalanceResponseDto.builder()
                .accountId(accountId)
                .currency("PHP")
                .availableBalance(new BigDecimal("5000.0000"))
                .asOfTimestamp(Instant.now())
                .isCached(false)
                .build();

        String cachedJson = objectMapper.writeValueAsString(cachedDto);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("ledger:balance:" + accountId)).thenReturn(cachedJson);

        mockMvc.perform(get("/api/v1/ledger/accounts/{accountId}/balance", accountId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(10))
                .andExpect(jsonPath("$.availableBalance").value(5000.0000))
                .andExpect(jsonPath("$.isCached").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/ledger/balance/{id} (alias endpoint): Returns 200 OK")
    void shouldSupportAliasBalanceEndpoint() throws Exception {
        Long accountId = 20L;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("ledger:balance:" + accountId)).thenReturn(null);

        BalanceResponseDto authoritativeDto = BalanceResponseDto.builder()
                .accountId(accountId)
                .currency("PHP")
                .availableBalance(new BigDecimal("1500.0000"))
                .asOfTimestamp(Instant.now())
                .isCached(false)
                .build();

        when(accountBalanceService.getAccountBalance(accountId)).thenReturn(authoritativeDto);

        mockMvc.perform(get("/api/v1/ledger/balance/{accountId}", accountId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(20))
                .andExpect(jsonPath("$.availableBalance").value(1500.0000));
    }

    @Test
    @DisplayName("GET /api/v1/ledger/accounts/{id}/balance: Returns 404 Not Found when account does not exist")
    void shouldReturn404WhenAccountNotFound() throws Exception {
        Long accountId = 999L;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("ledger:balance:" + accountId)).thenReturn(null);

        when(accountBalanceService.getAccountBalance(accountId))
                .thenThrow(new AccountNotFoundException("Account 999 was not found"));

        mockMvc.perform(get("/api/v1/ledger/accounts/{accountId}/balance", accountId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404));
    }
}
