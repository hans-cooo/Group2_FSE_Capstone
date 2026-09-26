package com.group2.fse.ledger_service.interceptor;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

/**
 * Filter that wraps HttpServletResponse in ContentCachingResponseWrapper
 * for downstream interceptors to cache the response body.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class ContentCachingResponseWrapperFilter extends OncePerRequestFilter {

    public static final String CACHED_RESPONSE_WRAPPER_ATTR = "CACHED_RESPONSE_WRAPPER_ATTR";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (response instanceof ContentCachingResponseWrapper) {
            filterChain.doFilter(request, response);
            return;
        }

        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        request.setAttribute(CACHED_RESPONSE_WRAPPER_ATTR, responseWrapper);

        try {
            filterChain.doFilter(request, responseWrapper);
        } finally {
            responseWrapper.copyBodyToResponse();
        }
    }
}
