package com.group2.fse.api_gateway.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    @DisplayName("Should generate new UUID correlation ID when header is missing")
    void shouldGenerateNewCorrelationIdWhenMissing() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/accounts/123").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicReference<String> downstreamHeader = new AtomicReference<>();
        GatewayFilterChain chain = mutatedExchange -> {
            downstreamHeader.set(mutatedExchange.getRequest().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER));
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        String generatedId = downstreamHeader.get();
        assertThat(generatedId).isNotNull().isNotBlank();
        assertDoesNotThrow(() -> UUID.fromString(generatedId), "Generated ID must be a valid UUID");

        HttpHeaders responseHeaders = exchange.getResponse().getHeaders();
        assertThat(responseHeaders.getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo(generatedId);
    }

    @Test
    @DisplayName("Should preserve existing correlation ID when provided by caller")
    void shouldPreserveExistingCorrelationId() {
        String existingId = "CORR-TEST-12345";
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/ledger/transfers")
                .header(CorrelationIdFilter.CORRELATION_ID_HEADER, existingId)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicReference<String> downstreamHeader = new AtomicReference<>();
        GatewayFilterChain chain = mutatedExchange -> {
            downstreamHeader.set(mutatedExchange.getRequest().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER));
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(downstreamHeader.get()).isEqualTo(existingId);
        HttpHeaders responseHeaders = exchange.getResponse().getHeaders();
        assertThat(responseHeaders.getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo(existingId);
    }
}
