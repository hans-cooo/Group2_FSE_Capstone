package com.group2.fse.ledger_service.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group2.fse.ledger_service.security.blacklist.TokenBlacklistService;
import com.group2.fse.ledger_service.security.config.CorsConfig;
import com.group2.fse.ledger_service.security.config.SecurityConfig;
import com.group2.fse.ledger_service.security.filter.JwtAuthenticationFilter;
import com.group2.fse.ledger_service.security.handler.CustomAccessDeniedHandler;
import com.group2.fse.ledger_service.security.handler.CustomAuthenticationEntryPoint;
import com.group2.fse.ledger_service.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("SecurityConfig Unit & Context Tests")
class SecurityConfigTest {

    @Configuration
    static class TestMockConfig {
        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        public JwtTokenProvider jwtTokenProvider() {
            return mock(JwtTokenProvider.class);
        }

        @Bean
        public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
            return new JwtAuthenticationFilter(tokenProvider);
        }

        @Bean
        public TokenBlacklistService tokenBlacklistService() {
            return mock(TokenBlacklistService.class);
        }

        @Bean
        public CustomAuthenticationEntryPoint customAuthenticationEntryPoint(ObjectMapper objectMapper) {
            return new CustomAuthenticationEntryPoint(objectMapper);
        }

        @Bean
        public CustomAccessDeniedHandler customAccessDeniedHandler(ObjectMapper objectMapper) {
            return new CustomAccessDeniedHandler(objectMapper);
        }
    }

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestMockConfig.class, CorsConfig.class, SecurityConfig.class);

    @Test
    @DisplayName("Should successfully load SecurityFilterChain and CorsConfigurationSource beans")
    void shouldLoadSecurityBeansSuccessfully() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(SecurityFilterChain.class);
            assertThat(context).hasSingleBean(CorsConfigurationSource.class);
            assertThat(context).hasSingleBean(JwtAuthenticationFilter.class);
        });
    }
}
