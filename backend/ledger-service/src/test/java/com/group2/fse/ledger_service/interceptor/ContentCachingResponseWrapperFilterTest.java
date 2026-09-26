package com.group2.fse.ledger_service.interceptor;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ContentCachingResponseWrapperFilter Unit Tests (FSE-307)")
class ContentCachingResponseWrapperFilterTest {

    private ContentCachingResponseWrapperFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new ContentCachingResponseWrapperFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("Should wrap response and copy cached body to response stream on completion")
    void shouldWrapResponseAndCopyBody() throws Exception {
        request.setRequestURI("/api/v1/ledger/transfers");

        FilterChain filterChain = (req, res) -> {
            assertTrue(res instanceof ContentCachingResponseWrapper);
            Object attr = req.getAttribute(ContentCachingResponseWrapperFilter.CACHED_RESPONSE_WRAPPER_ATTR);
            assertNotNull(attr);
            res.getOutputStream().write("{\"status\":\"COMPLETED\"}".getBytes(StandardCharsets.UTF_8));
        };

        filter.doFilter(request, response, filterChain);

        assertEquals("{\"status\":\"COMPLETED\"}", response.getContentAsString());
    }

    @Test
    @DisplayName("Should not re-wrap if response is already ContentCachingResponseWrapper")
    void shouldNotRewrapIfAlreadyWrapped() throws Exception {
        request.setRequestURI("/api/v1/ledger/transfers");
        ContentCachingResponseWrapper alreadyWrapped = new ContentCachingResponseWrapper(response);

        FilterChain filterChain = (req, res) -> {
            assertSame(alreadyWrapped, res);
            res.getOutputStream().write("OK".getBytes(StandardCharsets.UTF_8));
        };

        filter.doFilter(request, alreadyWrapped, filterChain);
        alreadyWrapped.copyBodyToResponse();

        assertEquals("OK", response.getContentAsString());
    }
}
