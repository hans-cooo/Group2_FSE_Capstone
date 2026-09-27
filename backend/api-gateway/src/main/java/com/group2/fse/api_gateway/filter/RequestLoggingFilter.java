package com.group2.fse.api_gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter that captures ingress request metrics, client IP, route path,
 * correlation ID, response HTTP status code, and latency in milliseconds.
 */
@Slf4j
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        String path = exchange.getRequest().getURI().getRawPath();
        String method = exchange.getRequest().getMethod().name();
        String correlationId = exchange.getRequest().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);

        log.info("[GATEWAY-IN] [{}] {} {} from {}",
                correlationId,
                method,
                path,
                exchange.getRequest().getRemoteAddress());

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long duration = System.currentTimeMillis() - startTime;
            HttpStatusCode status = exchange.getResponse().getStatusCode();
            log.info("[GATEWAY-OUT] [{}] {} {} -> Status: {} ({}ms)",
                    correlationId,
                    method,
                    path,
                    status != null ? status.value() : "UNKNOWN",
                    duration);
        }));
    }

    @Override
    public int getOrder() {
        // Execute immediately after CorrelationIdFilter so correlation ID is present in logs
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
