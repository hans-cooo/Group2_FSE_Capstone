package com.group2.fse.auth_service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("Phase 1: AuthService Application Context & Scaffolding Smoke Tests")
class AuthServiceApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    @DisplayName("Should successfully load ApplicationContext with all Phase 1 beans")
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
    }

    @Test
    @DisplayName("Should wire Oracle XE DataSource bean correctly")
    void shouldWireDataSource() {
        assertThat(dataSource).isNotNull();
    }

    @Test
    @DisplayName("Should configure BCryptPasswordEncoder bean")
    void shouldConfigurePasswordEncoder() {
        assertThat(passwordEncoder).isNotNull();
        String rawPassword = "Password123!";
        String encoded = passwordEncoder.encode(rawPassword);
        assertThat(passwordEncoder.matches(rawPassword, encoded)).isTrue();
    }

    @Test
    @DisplayName("Should configure RedisTemplate and StringRedisTemplate beans")
    void shouldConfigureRedisTemplates() {
        assertThat(redisTemplate).isNotNull();
        assertThat(stringRedisTemplate).isNotNull();
    }
}
