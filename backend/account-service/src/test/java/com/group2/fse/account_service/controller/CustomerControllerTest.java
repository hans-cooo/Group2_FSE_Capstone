package com.group2.fse.account_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.group2.fse.account_service.dto.*;
import com.group2.fse.account_service.exception.CustomerNotFoundException;
import com.group2.fse.account_service.exception.GlobalExceptionHandler;
import com.group2.fse.account_service.security.jwt.UserPrincipal;
import com.group2.fse.account_service.service.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Customer Controller REST API Contract Tests")
class CustomerControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private CustomerController customerController;

    private UserPrincipal customerPrincipal;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        customerPrincipal = UserPrincipal.create(101L, "john_doe", List.of("ROLE_CUSTOMER"));

        mockMvc = MockMvcBuilders.standaloneSetup(customerController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return customerPrincipal;
                    }
                })
                .build();
    }

    @Test
    @DisplayName("[API-CUST-001] GET /api/v1/customers/me -> 200 OK with authenticated customer profile")
    void shouldGetCustomerProfile() throws Exception {
        CustomerProfileResponse profile = CustomerProfileResponse.builder()
                .customerId(101L)
                .username("john_doe")
                .email("john.doe@example.com")
                .kycStatus("VERIFIED")
                .createdAt(LocalDateTime.now())
                .build();

        when(customerService.getCustomerProfile(101L)).thenReturn(profile);

        mockMvc.perform(get("/api/v1/customers/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(101))
                .andExpect(jsonPath("$.username").value("john_doe"))
                .andExpect(jsonPath("$.kycStatus").value("VERIFIED"));
    }

    @Test
    @DisplayName("[API-CUST-002] GET /api/v1/customers/{id} -> 404 Not Found RFC-7807 when customer does not exist")
    void shouldReturn404WhenCustomerNotFound() throws Exception {
        when(customerService.getCustomerById(999L))
                .thenThrow(new CustomerNotFoundException("Customer not found with ID: 999"));

        mockMvc.perform(get("/api/v1/customers/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CUSTOMER_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("[API-CUST-003] POST /api/v1/customers/kyc/update-request -> 202 Accepted on valid staged update")
    void shouldAcceptKycUpdateRequest() throws Exception {
        KycUpdateRequestDto request = KycUpdateRequestDto.builder()
                .newAddress("456 BGC High Street, Taguig City")
                .newMobileNumber("+639189998888")
                .newOccupation("Senior Architect")
                .build();

        KycRequestResponse response = KycRequestResponse.builder()
                .kycRequestId(55L)
                .status("PENDING")
                .message("KYC update request submitted for teller approval.")
                .build();

        when(customerService.submitKycUpdateRequest(eq(101L), any(KycUpdateRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/customers/kyc/update-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.kycRequestId").value(55))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("[API-CUST-004] POST /api/v1/customers/kyc/update-requests/{id}/approve -> 200 OK")
    void shouldApproveKycUpdateRequest() throws Exception {
        KycRequestResponse response = KycRequestResponse.builder()
                .kycRequestId(55L)
                .status("APPROVED")
                .approvedBy(101L)
                .message("Customer KYC profile successfully updated.")
                .build();

        when(customerService.approveKycUpdateRequest(55L, 101L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/customers/kyc/update-requests/55/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kycRequestId").value(55))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }
}
