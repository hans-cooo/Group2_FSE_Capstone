package com.group2.fse.ledger_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Task: FSE-301
 * Assigned to: Carl
 * Redis configuration for distributed atomic SETNX idempotency locks and result caching.
 */
@Configuration
public class RedisConfig {

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        template.setKeySerializer(org.springframework.data.redis.serializer.RedisSerializer.string());
        template.setHashKeySerializer(org.springframework.data.redis.serializer.RedisSerializer.string());
        template.setValueSerializer(org.springframework.data.redis.serializer.RedisSerializer.json());
        template.setHashValueSerializer(org.springframework.data.redis.serializer.RedisSerializer.json());

        template.afterPropertiesSet();
        return template;
    }
}
