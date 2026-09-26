package com.group2.fse.ledger_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group2.fse.ledger_service.dto.TransferRequestDto;
import com.group2.fse.ledger_service.dto.TransferResponseDto;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Ledger Transfer Controller Unit Tests (FSE-403)")
class LedgerTransferControllerTest {

    @Mock
    private AccountBalanceService accountBalanceService;

    @InjectMocks
    private LedgerTransferController controller;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new com.group2.fse.ledger_service.config.JacksonConfig().objectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/ledger/transfers: Returns 201 Created on valid atomic transfer")
    void shouldReturn201OnValidTransfer() throws Exception {
        TransferRequestDto requestDto = TransferRequestDto.builder()
                .sourceAccountId(10L)
                .destinationAccountId(20L)
                .amount(new BigDecimal("500.0000"))
                .referenceNo("TRF-20260926-001")
                .remarks("Peer to peer transfer")
                .build();

        TransferResponseDto responseDto = TransferResponseDto.builder()
                .transferReference("TRF-20260926-001")
                .sourceAccountId(10L)
                .destinationAccountId(20L)
                .amount(new BigDecimal("500.0000"))
                .sourceNewBalance(new BigDecimal("4500.0000"))
                .destinationNewBalance(new BigDecimal("1500.0000"))
                .status("COMPLETED")
                .timestamp(Instant.now())
                .build();

        when(accountBalanceService.executeTransfer(any(TransferRequestDto.class), any(), any()))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/ledger/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transferReference").value("TRF-20260926-001"))
                .andExpect(jsonPath("$.sourceAccountId").value(10))
                .andExpect(jsonPath("$.destinationAccountId").value(20))
                .andExpect(jsonPath("$.amount").value(500.0000))
                .andExpect(jsonPath("$.sourceNewBalance").value(4500.0000))
                .andExpect(jsonPath("$.destinationNewBalance").value(1500.0000))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /api/v1/ledger/transfer (singular alias): Returns 201 Created")
    void shouldSupportSingularTransferAlias() throws Exception {
        TransferRequestDto requestDto = TransferRequestDto.builder()
                .sourceAccountId(10L)
                .destinationAccountId(20L)
                .amount(new BigDecimal("200.0000"))
                .referenceNo("TRF-20260926-002")
                .build();

        TransferResponseDto responseDto = TransferResponseDto.builder()
                .transferReference("TRF-20260926-002")
                .sourceAccountId(10L)
                .destinationAccountId(20L)
                .amount(new BigDecimal("200.0000"))
                .sourceNewBalance(new BigDecimal("4300.0000"))
                .destinationNewBalance(new BigDecimal("1700.0000"))
                .status("COMPLETED")
                .timestamp(Instant.now())
                .build();

        when(accountBalanceService.executeTransfer(any(TransferRequestDto.class), any(), any()))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/ledger/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transferReference").value("TRF-20260926-002"));
    }

    @Test
    @DisplayName("POST /api/v1/ledger/transfers: Returns 422 Unprocessable Entity when source funds insufficient")
    void shouldReturn422WhenInsufficientFunds() throws Exception {
        TransferRequestDto requestDto = TransferRequestDto.builder()
                .sourceAccountId(10L)
                .destinationAccountId(20L)
                .amount(new BigDecimal("10000.0000"))
                .referenceNo("TRF-FAIL-001")
                .build();

        when(accountBalanceService.executeTransfer(any(TransferRequestDto.class), any(), any()))
                .thenThrow(new InsufficientFundsException(10L, new BigDecimal("500.0000"), new BigDecimal("10000.0000")));

        mockMvc.perform(post("/api/v1/ledger/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_FUNDS"))
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    @DisplayName("POST /api/v1/ledger/transfers: Returns 400 Bad Request when source equals destination")
    void shouldReturn400WhenSourceEqualsDestination() throws Exception {
        TransferRequestDto invalidDto = TransferRequestDto.builder()
                .sourceAccountId(10L)
                .destinationAccountId(10L) // same account
                .amount(new BigDecimal("100.0000"))
                .referenceNo("TRF-SAME-ACC")
                .build();

        mockMvc.perform(post("/api/v1/ledger/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST_PARAMETERS"));
    }
}
