package com.jerokysys.academic.resources.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
@ConditionalOnProperty(name = "cache.enabled", havingValue = "true")
public class RedisCacheConfig {
    public static final String CACHE_DISCIPLINE = "discipline";
    public static final String CACHE_DANCE_CLASS = "danceclass";
    public static final String CACHE_ENROLLMENT = "enrollment";
    public static final String CACHE_EXAM = "exam";
    public static final String CACHE_EXAM_RESULT = "examresult";

    @Bean
    public CacheManager cacheManager(
            RedisConnectionFactory factory,
            @Value("${cache.discipline.ttl-seconds:600}") long disciplineTtl,
            @Value("${cache.danceclass.ttl-seconds:300}") long danceClassTtl,
            @Value("${cache.enrollment.ttl-seconds:120}") long enrollmentTtl,
            @Value("${cache.exam.ttl-seconds:300}") long examTtl,
            @Value("${cache.examresult.ttl-seconds:120}") long examResultTtl) {

        GenericJackson2JsonRedisSerializer json = new GenericJackson2JsonRedisSerializer();
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(json));

        Map<String, RedisCacheConfiguration> perCache = new HashMap<>();
        perCache.put(CACHE_DISCIPLINE, defaults.entryTtl(Duration.ofSeconds(disciplineTtl)));
        perCache.put(CACHE_DANCE_CLASS, defaults.entryTtl(Duration.ofSeconds(danceClassTtl)));
        perCache.put(CACHE_ENROLLMENT, defaults.entryTtl(Duration.ofSeconds(enrollmentTtl)));
        perCache.put(CACHE_EXAM, defaults.entryTtl(Duration.ofSeconds(examTtl)));
        perCache.put(CACHE_EXAM_RESULT, defaults.entryTtl(Duration.ofSeconds(examResultTtl)));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(perCache)
                .build();
    }
}