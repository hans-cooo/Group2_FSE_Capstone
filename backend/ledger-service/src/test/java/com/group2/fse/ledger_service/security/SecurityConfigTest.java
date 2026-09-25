package com.group2.fse.ledger_service.security;

import com.group2.fse.ledger_service.security.config.CorsConfig;
import com.group2.fse.ledger_service.security.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SecurityConfig Unit Tests (Carl - FSE-401)")
class SecurityConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(CorsConfig.class, SecurityConfig.class);

    @Test
    @DisplayName("Should successfully load SecurityFilterChain and CorsConfigurationSource beans")
    void shouldLoadSecurityBeansSuccessfully() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(SecurityFilterChain.class);
            assertThat(context).hasSingleBean(CorsConfigurationSource.class);
        });
    }
}
