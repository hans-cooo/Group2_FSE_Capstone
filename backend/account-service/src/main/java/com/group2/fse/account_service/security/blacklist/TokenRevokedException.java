package com.group2.fse.account_service.security.blacklist;

import org.springframework.security.core.AuthenticationException;

public class TokenRevokedException extends AuthenticationException {
    public TokenRevokedException(String msg) {
        super(msg);
    }

    public TokenRevokedException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
