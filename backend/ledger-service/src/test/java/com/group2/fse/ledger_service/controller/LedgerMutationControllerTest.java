package com.group2.fse.ledger_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group2.fse.ledger_service.dto.DebitCreditRequestDto;
import com.group2.fse.ledger_service.dto.DebitCreditResponseDto;
import com.group2.fse.ledger_service.exception.GlobalExceptionHandler;
import com.group2.fse.ledger_service.exception.InsufficientFundsException;
import com.group2.fse.ledger_service.service.AccountBalanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Ledger Mutation Controller Unit Tests (FSE-403)")
class LedgerMutationControllerTest {

    @Mock
    private AccountBalanceService accountBalanceService;

    @InjectMocks
    private LedgerMutationController controller;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new com.group2.fse.ledger_service.config.JacksonConfig().objectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/ledger/debits: Returns 201 Created on valid debit")
    void shouldReturn201OnValidDebit() throws Exception {
        DebitCreditRequestDto requestDto = DebitCreditRequestDto.builder()
                .accountId(10L)
                .amount(new BigDecimal("250.0000"))
                .referenceNo("DEB-20260926-001")
                .remarks("ATM withdrawal")
                .build();

        DebitCreditResponseDto responseDto = DebitCreditResponseDto.builder()
                .transactionId(1001L)
                .accountId(10L)
                .transactionType("DEBIT")
                .amount(new BigDecimal("250.0000"))
                .previousBalance(new BigDecimal("1000.0000"))
                .newBalance(new BigDecimal("750.0000"))
                .status("COMPLETED")
                .referenceNo("DEB-20260926-001")
                .timestamp(Instant.now())
                .build();

        when(accountBalanceService.mutateDebit(any(DebitCreditRequestDto.class), any(), any()))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/ledger/debits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").value(1001))
                .andExpect(jsonPath("$.accountId").value(10))
                .andExpect(jsonPath("$.transactionType").value("DEBIT"))
                .andExpect(jsonPath("$.amount").value(250.0000))
                .andExpect(jsonPath("$.newBalance").value(750.0000))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /api/v1/ledger/debit (singular alias): Returns 201 Created")
    void shouldSupportSingularDebitAlias() throws Exception {
        DebitCreditRequestDto requestDto = DebitCreditRequestDto.builder()
                .accountId(10L)
                .amount(new BigDecimal("100.0000"))
                .referenceNo("DEB-20260926-002")
                .build();

        DebitCreditResponseDto responseDto = DebitCreditResponseDto.builder()
                .transactionId(1002L)
                .accountId(10L)
                .transactionType("DEBIT")
                .amount(new BigDecimal("100.0000"))
                .previousBalance(new BigDecimal("750.0000"))
                .newBalance(new BigDecimal("650.0000"))
                .status("COMPLETED")
                .referenceNo("DEB-20260926-002")
                .timestamp(Instant.now())
                .build();

        when(accountBalanceService.mutateDebit(any(DebitCreditRequestDto.class), any(), any()))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/ledger/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").value(1002));
    }

    @Test
    @DisplayName("POST /api/v1/ledger/credits: Returns 201 Created on valid credit")
    void shouldReturn201OnValidCredit() throws Exception {
        DebitCreditRequestDto requestDto = DebitCreditRequestDto.builder()
                .accountId(20L)
                .amount(new BigDecimal("500.0000"))
                .referenceNo("CRD-20260926-001")
                .remarks("Cash deposit")
                .build();

        DebitCreditResponseDto responseDto = DebitCreditResponseDto.builder()
                .transactionId(1003L)
                .accountId(20L)
                .transactionType("CREDIT")
                .amount(new BigDecimal("500.0000"))
                .previousBalance(new BigDecimal("2000.0000"))
                .newBalance(new BigDecimal("2500.0000"))
                .status("COMPLETED")
                .referenceNo("CRD-20260926-001")
                .timestamp(Instant.now())
                .build();

        when(accountBalanceService.mutateCredit(any(DebitCreditRequestDto.class), any(), any()))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/ledger/credits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").value(1003))
                .andExpect(jsonPath("$.accountId").value(20))
                .andExpect(jsonPath("$.transactionType").value("CREDIT"))
                .andExpect(jsonPath("$.newBalance").value(2500.0000));
    }

    @Test
    @DisplayName("POST /api/v1/ledger/debits: Returns 422 Unprocessable Entity when funds insufficient")
    void shouldReturn422WhenInsufficientFunds() throws Exception {
        DebitCreditRequestDto requestDto = DebitCreditRequestDto.builder()
                .accountId(10L)
                .amount(new BigDecimal("9999.0000"))
                .referenceNo("DEB-FAIL-001")
                .build();

        when(accountBalanceService.mutateDebit(any(DebitCreditRequestDto.class), any(), any()))
                .thenThrow(new InsufficientFundsException(10L, new BigDecimal("100.0000"), new BigDecimal("9999.0000")));

        mockMvc.perform(post("/api/v1/ledger/debits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_FUNDS"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.accountId").value(10))
                .andExpect(jsonPath("$.attemptedAmount").value(9999.0000));
    }

    @Test
    @DisplayName("POST /api/v1/ledger/debits: Returns 400 Bad Request on invalid DTO validation")
    void shouldReturn400OnInvalidParameters() throws Exception {
        DebitCreditRequestDto invalidDto = DebitCreditRequestDto.builder()
                .accountId(null) // invalid null
                .amount(new BigDecimal("-50.0000")) // invalid negative
                .referenceNo("") // invalid blank
                .build();

        mockMvc.perform(post("/api/v1/ledger/debits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST_PARAMETERS"))
                .andExpect(jsonPath("$.invalidParams").isArray());
    }
}
