package com.group2.fse.account_service.service;

import com.group2.fse.account_service.dto.*;
import org.springframework.data.domain.Pageable;

public interface CustomerService {
    CustomerProfileResponse getCustomerProfile(Long customerId);
    CustomerProfileResponse getCustomerById(Long customerId);
    KycRequestResponse submitKyc(Long customerId, KycSubmitRequest request);
    KycRequestResponse submitKycUpdateRequest(Long customerId, KycUpdateRequestDto request);
    PageResponse<KycRequestResponse> getPendingKycUpdateRequests(String status, Pageable pageable);
    KycRequestResponse approveKycUpdateRequest(Long requestId, Long staffUserId);
    KycRequestResponse rejectKycUpdateRequest(Long requestId, Long staffUserId, String rejectionReason);
}
