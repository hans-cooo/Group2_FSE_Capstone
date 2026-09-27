package com.group2.fse.account_service.service;

import com.group2.fse.account_service.dto.AccountClosureRequestDto;
import com.group2.fse.account_service.dto.ClosureRequestResponse;
import com.group2.fse.account_service.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface AccountClosureService {
    ClosureRequestResponse submitClosureRequest(Long accountId, Long customerId, AccountClosureRequestDto request);
    ClosureRequestResponse getClosureRequestForAccount(Long accountId, Long customerId);
    PageResponse<ClosureRequestResponse> getPendingClosureRequests(String status, Pageable pageable);
    ClosureRequestResponse approveClosureRequest(Long requestId, Long adminUserId);
    ClosureRequestResponse rejectClosureRequest(Long requestId, Long adminUserId, String rejectionReason);
}
