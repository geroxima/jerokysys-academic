package com.jerokysys.academic.config;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.BatchStrategies;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis cache for academic, organised by domains (sections). Each domain has
 * its own TTL in application.properties and is invalidated without touching
 * the others. Redis keys: academic:<domain>:<id>.
 */
@Configuration
@EnableConfigurationProperties(RedisCacheConfig.Properties.class)
public class RedisCacheConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheConfig.class);

    public static final String CACHE_DISCIPLINE = "discipline";
    public static final String CACHE_DANCE_CLASS = "danceclass";
    public static final String CACHE_ENROLLMENT = "enrollment";
    public static final String CACHE_EXAM = "exam";
    public static final String CACHE_EXAM_RESULT = "examresult";

    /**
     * Every value comes from application.properties (prefix "cache").
     *
     * @param enabled        false turns the whole cache off, every read goes to the database
     * @param keyPrefix      start of every Redis key, for example "academic"
     * @param defaultTtlSeconds life of a domain that has no entry in domains
     * @param scanBatchSize  keys per SCAN batch when a whole domain is cleared
     * @param domains        TTL per domain in seconds; 0 = persistent (no expiration)
     */
    @ConfigurationProperties(prefix = "cache")
    public record Properties(boolean enabled, String keyPrefix, long defaultTtlSeconds,
                             int scanBatchSize, Map<String, Long> domains) {

        public Properties {
            if (keyPrefix == null || keyPrefix.isBlank()) {
                throw new IllegalStateException("cache.key-prefix is required, for example: academic");
            }
            if (domains == null || domains.isEmpty()) {
                throw new IllegalStateException("At least one cache.domains.<name>=<seconds> entry is required");
            }
            if (defaultTtlSeconds <= 0) {
                defaultTtlSeconds = 120;
            }
            scanBatchSize = scanBatchSize <= 0 ? 100 : scanBatchSize;
            domains = Map.copyOf(domains);
        }

        public Duration ttl(String domain) {
            return Duration.ofSeconds(domains.getOrDefault(domain, defaultTtlSeconds));
        }
    }

    @Bean
    public CacheManager cacheManager(Properties properties, RedisConnectionFactory factory) {
        if (!properties.enabled()) {
            log.warn("Cache disabled (cache.enabled=false): every read goes to the database");
            return new NoOpCacheManager();
        }

        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .computePrefixWith(name -> properties.keyPrefix() + ":" + name + ":")
                .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(SerializationPair.fromSerializer(valueSerializer()));

        Map<String, RedisCacheConfiguration> configs = new LinkedHashMap<>();
        properties.domains().forEach((name, seconds) ->
                configs.put(name, base.entryTtl(Duration.ofSeconds(seconds))));

        RedisCacheManager redis = RedisCacheManager
                .builder(RedisCacheWriter.nonLockingRedisCacheWriter(
                        factory, BatchStrategies.scan(properties.scanBatchSize())))
                .cacheDefaults(base.entryTtl(Duration.ofSeconds(properties.defaultTtlSeconds())))
                .withInitialCacheConfigurations(configs)
                // Only the domains declared in application.properties exist: a name
                // that is not declared fails instead of silently creating a section.
                .disableCreateOnMissingCache()
                .build();
        redis.initializeCaches();

        properties.domains().forEach((name, seconds) -> log.info("Cache '{}' -> keys {}:{}: ({})",
                name, properties.keyPrefix(), name, seconds == 0 ? "persistent" : "TTL " + seconds + "s"));

        return new LoggingCache.Manager(redis, properties);
    }

    /**
     * Redis accelerates, it never decides: when it fails, the operation
     * continues against PostgreSQL and the failure is logged.
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache unavailable on GET {}; reading the database ({})",
                        displayKey(cache, key), exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Cache unavailable on SET {} ({})", displayKey(cache, key), exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.error("Cache unavailable on DELETE {}; the entry can stay stale until its TTL ({})",
                        displayKey(cache, key), exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.error("Cache unavailable while clearing {}; entries can stay stale until their TTL ({})",
                        cache.getName(), exception.getMessage());
            }
        };
    }

    private static String displayKey(Cache cache, Object key) {
        return cache instanceof LoggingCache logging ? logging.fullKey(key) : cache.getName() + "::" + key;
    }

    /** JSON values with type information, so a DTO is read back as its own type. */
    public static RedisSerializer<Object> valueSerializer() {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mapper.activateDefaultTyping(
                mapper.getPolymorphicTypeValidator(),
                com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping.NON_FINAL,
                com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY);
        return new GenericJackson2JsonRedisSerializer(mapper);
    }
}