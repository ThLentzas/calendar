package org.example.calendar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.example.calendar.auth.dto.RefreshToken;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
class RedisConfig {

    /*
        RedisConfig is not a SpringBean but RedisTemplate is, so Spring will try to satisfy the Bean dependencies and
        inject the autoconfigured ObjectMapper.

        The following won't work.
            private final ObjectMapper mapper;
            RedisConfig(ObjectMapper mapper) {
                this.mapper = mapper;
            }
        RedisConfig is not a Bean managed by Spring so, it will not inject the ObjectMapper.
        https://docs.spring.io/spring-framework/reference/core/beans/java/composing-configuration-classes.html
     */
    @Bean
    RedisTemplate<String, RefreshToken> redisTemplate(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        Jackson2JsonRedisSerializer<RefreshToken> serializer = new Jackson2JsonRedisSerializer<>(objectMapper, RefreshToken.class);
        RedisTemplate<String, RefreshToken> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);

        return template;
    }
}
