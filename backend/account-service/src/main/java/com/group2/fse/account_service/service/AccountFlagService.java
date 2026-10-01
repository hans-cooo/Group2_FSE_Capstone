package com.group2.fse.account_service.service;

import com.group2.fse.account_service.dto.AccountFlagRequest;
import com.group2.fse.account_service.dto.AccountFlagResponse;

import java.util.List;

public interface AccountFlagService {
    AccountFlagResponse addFlag(Long accountId, AccountFlagRequest request, Long staffUserId);
    AccountFlagResponse removeFlag(Long accountId, Long flagId, Long adminUserId);
    List<AccountFlagResponse> getFlagsForAccount(Long accountId);
}
