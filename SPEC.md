# SPEC — Redis cache with logging for `academic`

Status: ready to implement
Scope: `academic` only (`jerokysys-auth`, `jerokysys-invoices`, `commons`, `api-gateway` are out of scope)
Requirement: project requirement set 2 (CACHÉ)

---

## 1. Goal

Give `academic` a Redis cache that satisfies all ten requirements of the CACHÉ
specification: cache on the API side, one Redis per microservice, every object
reachable through the cache, segmentation by domains, Spring annotations plus a
programmatic `CacheManager`, a log line for every operation, configuration only
in external files, a clear key strategy, no caching of lists or pages, and an
explicit retention policy.

## 2. Non-goals

- No changes to `commons`, `jerokysys-auth`, `jerokysys-invoices`, `api-gateway` or `pom.xml`.
  `spring-boot-starter-data-redis` is already a dependency.
- No change to the value serializer. Values keep the current
  `GenericJackson2JsonRedisSerializer()` format.
- No caching of list or paginated query results as a whole.
- No Redis server configuration file. The instance on `localhost:6379` is external.

## 3. Current state

| Fact | Location |
|---|---|
| `RedisCacheConfig` builds a `RedisCacheManager` with 5 caches and per-cache TTL, gated by `@ConditionalOnProperty(cache.enabled)` | `src/main/java/com/jerokysys/academic/config/RedisCacheConfig.java` |
| `@EnableCaching` | `AcademicApplication.java` |
| 5 services annotate `getById`, `getAll`, `create`, `update`, `delete` | `src/main/java/com/jerokysys/academic/services/*.java` |
| `getAll()` is `@Cacheable(key = "'<domain>:all'")`, which stores a whole list | all 5 services |
| No log output per operation, no startup output, no fallback when Redis is down | — |
| `src/test/java` is empty | — |

## 4. Locked decisions

| Decision | Value |
|---|---|
| Code placement | Three classes in `academic`, no shared library |
| `getAll()` | Database read, then each element stored individually through `DomainCache` |
| Redis | One instance per microservice, `localhost:6379`, `database=0` |
| Key layout | `<key-prefix>:<domain>:<id>` → `academic:enrollment:5` |
| Class name | Keep `RedisCacheConfig`, so the 5 existing imports do not change |
| Retention | `discipline` persistent (`0`), other four keep their current TTLs |
| Warming | Only from `getAll()`. `search`, `getByFilter` and `getBy*` stay plain reads |

## 5. Requirements → design

| # | Requirement | Design answer |
|---|---|---|
| R1 | Cache management on the API side | Annotations in the service layer, `CacheManager` in `config/`. Nothing outside the service |
| R2 | One Redis instance per microservice | Own `spring.data.redis.*` block, `localhost:6379`, `database=0`, env-overridable |
| R3 | All objects managed by the cache | Read by id → `@Cacheable`, write → `@CachePut` / `@CacheEvict`, list read → `DomainCache.putAll` |
| R4 | Segmentation by domains | One cache per `cache.domains.*` line, evicted independently |
| R5 | Spring annotations + programmatic `CacheManager` | Annotations for CRUD, `DomainCache` calls `cacheManager.getCache(name)` directly |
| R6 | Log every operation | `LoggingCache` decorator: `GET/SET/PUTIFABSENT/DELETE/CLEAR` with the real key, HIT/MISS and TTL, plus one startup line per domain |
| R7 | Configuration in external files | 10 properties, no literal in Java. An undeclared domain fails at startup |
| R8 | Clear, representative, consistent keys | `<service>:<domain>:<id>`, one id source per method |
| R9 | No lists or pages in the cache | `@Cacheable` removed from `getAll`, no annotation on any list query, elements stored one key each |
| R10 | Explicit retention policy | Per-domain seconds in properties, `0` = persistent, matrix in section 11 |

**The rule that resolves R3 against R9:** a list never enters the cache as a
whole, but every element of a list does, under its own key. `getAll()` runs one
SQL query and warms N entries.

## 6. Files

| Action | File |
|---|---|
| create | `src/main/java/com/jerokysys/academic/config/LoggingCache.java` |
| create | `src/main/java/com/jerokysys/academic/config/DomainCache.java` |
| rewrite | `src/main/java/com/jerokysys/academic/config/RedisCacheConfig.java` |
| edit | `src/main/java/com/jerokysys/academic/services/EnrollmentService.java` |
| edit | `src/main/java/com/jerokysys/academic/services/DanceClassService.java` |
| edit | `src/main/java/com/jerokysys/academic/services/DisciplineService.java` |
| edit | `src/main/java/com/jerokysys/academic/services/ExamService.java` |
| edit | `src/main/java/com/jerokysys/academic/services/ExamResultService.java` |
| edit | `src/main/resources/application.properties` |
| edit | `README.md` (cache section, section 12) |

---

## 7. `LoggingCache.java` (new)

Requirement R6. Decorator plus a `CacheManager` wrapper that wraps each cache
exactly once. Writes through SLF4J, so the lines reach `app.log` and the
console with the existing `%X{traceId}` pattern.

```java
package com.jerokysys.academic.config;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Callable;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/**
 * Registers every cache operation with the real Redis key: GET with HIT/MISS,
 * SET with its TTL, DELETE with the key or the whole domain.
 */
public class LoggingCache implements Cache {

    private static final Logger log = LoggerFactory.getLogger(LoggingCache.class);

    private final Cache delegate;
    private final String prefix;   // "academic:enrollment:"
    private final Duration ttl;

    public LoggingCache(Cache delegate, String prefix, Duration ttl) {
        this.delegate = delegate;
        this.prefix = prefix;
        this.ttl = ttl;
    }

    /** The full Redis key, also used by the error handler. */
    String fullKey(Object key) {
        return prefix + key;
    }

    private String ttlLabel() {
        return (ttl == null || ttl.isZero()) ? "persistent" : "TTL " + ttl;
    }

    private void logGet(Object key, boolean hit) {
        log.info("Cache GET {} -> {}", fullKey(key), hit ? "HIT" : "MISS");
    }

    private void logSet(Object key) {
        log.info("Cache SET {} ({})", fullKey(key), ttlLabel());
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public Object getNativeCache() {
        return delegate.getNativeCache();
    }

    @Override
    public ValueWrapper get(Object key) {
        ValueWrapper value = delegate.get(key);
        logGet(key, value != null);
        return value;
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        T value = delegate.get(key, type);
        logGet(key, value != null);
        return value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Callable<T> valueLoader) {
        ValueWrapper cached = delegate.get(key);
        if (cached != null) {
            logGet(key, true);
            return (T) cached.get();
        }
        logGet(key, false);
        T value = delegate.get(key, valueLoader);
        logSet(key);
        return value;
    }

    @Override
    public CompletableFuture<?> retrieve(Object key) {
        CompletableFuture<?> future = delegate.retrieve(key);
        if (future == null) {
            logGet(key, false);
            return null;
        }
        return future.whenComplete((value, error) -> logGet(key, value != null));
    }

    @Override
    public <T> CompletableFuture<T> retrieve(Object key, Supplier<CompletableFuture<T>> valueLoader) {
        CompletableFuture<T> future = delegate.retrieve(key, valueLoader);
        if (future == null) {
            logGet(key, false);
            return null;
        }
        return future.whenComplete((value, error) -> logGet(key, value != null));
    }

    @Override
    public void put(Object key, Object value) {
        delegate.put(key, value);
        logSet(key);
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        ValueWrapper previous = delegate.putIfAbsent(key, value);
        if (previous == null) {
            logSet(key);
        }
        return previous;
    }

    @Override
    public void evict(Object key) {
        delegate.evict(key);
        log.info("Cache DELETE {}", fullKey(key));
    }

    @Override
    public boolean evictIfPresent(Object key) {
        boolean removed = delegate.evictIfPresent(key);
        log.info("Cache DELETE {} ({})", fullKey(key), removed ? "existed" : "absent");
        return removed;
    }

    @Override
    public void clear() {
        delegate.clear();
        log.info("Cache DELETE {}* (domain)", prefix);
    }

    @Override
    public boolean invalidate() {
        boolean removed = delegate.invalidate();
        log.info("Cache DELETE {}* (domain)", prefix);
        return removed;
    }

    /** Delivers every cache of the real manager already wrapped. */
    public static class Manager implements CacheManager {

        private final CacheManager delegate;
        private final RedisCacheConfig.Properties properties;
        private final Map<String, Cache> caches = new ConcurrentHashMap<>();

        public Manager(CacheManager delegate, RedisCacheConfig.Properties properties) {
            this.delegate = delegate;
            this.properties = properties;
        }

        @Override
        public Cache getCache(String name) {
            Cache existing = caches.get(name);
            if (existing != null) {
                return existing;
            }
            Cache target = delegate.getCache(name);
            if (target == null) {
                return null;
            }
            String prefix = properties.keyPrefix() + ":" + name + ":";
            return caches.computeIfAbsent(name,
                    n -> new LoggingCache(target, prefix, properties.ttl(n)));
        }

        @Override
        public Collection<String> getCacheNames() {
            return delegate.getCacheNames();
        }
    }
}
```

Note: `retrieve` is overridden on purpose. Spring routes some reads through it,
so skipping it would hide reads from the log.

---

## 8. `DomainCache.java` (new)

Requirement R5, programmatic use of the `CacheManager`, and half of R9: it
stores each element of a list result under its own key.

```java
package com.jerokysys.academic.config;

import java.util.Collection;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * Programmatic access to the cache, for what annotations cannot express:
 * storing each element of a list result individually instead of the list.
 * A Redis failure is logged and never breaks the request.
 */
@Component
public class DomainCache {

    private static final Logger log = LoggerFactory.getLogger(DomainCache.class);

    private final CacheManager cacheManager;

    public DomainCache(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /** Stores every element under its own key. Items or keys with a null key are skipped. */
    public <T> void putAll(String domain, Collection<T> items, Function<T, ?> keyOf) {
        Cache cache = require(domain);
        if (items == null || items.isEmpty()) {
            return;
        }
        int stored = 0;
        try {
            for (T item : items) {
                if (item == null) {
                    continue;
                }
                Object key = keyOf.apply(item);
                if (key != null) {
                    cache.put(key, item);
                    stored++;
                }
            }
        } catch (RuntimeException ex) {
            log.warn("Cache SET {}::* failed for {} of {} items: {}", domain, stored, items.size(), ex.getMessage());
        }
    }

    public void put(String domain, Object key, Object value) {
        if (key == null || value == null) {
            return;
        }
        Cache cache = require(domain);
        try {
            cache.put(key, value);
        } catch (RuntimeException ex) {
            log.warn("Cache SET {} failed: {}", cache.getName() + "::" + key, ex.getMessage());
        }
    }

    public void evict(String domain, Object key) {
        Cache cache = require(domain);
        try {
            cache.evict(key);
        } catch (RuntimeException ex) {
            log.warn("Cache DELETE {} failed: {}", cache.getName() + "::" + key, ex.getMessage());
        }
    }

    public void evictAll(String domain) {
        Cache cache = require(domain);
        try {
            cache.clear();
        } catch (RuntimeException ex) {
            log.warn("Cache DELETE {}::* failed: {}", cache.getName(), ex.getMessage());
        }
    }

    /**
     * Fails loudly when the domain is not declared in application.properties.
     * Deliberately outside the try blocks: a typo in a domain name must stop
     * the request instead of being logged and swallowed.
     */
    private Cache require(String domain) {
        Cache cache = cacheManager.getCache(domain);
        if (cache == null) {
            throw new IllegalStateException("Unknown cache domain '" + domain
                    + "'. Add cache.domains." + domain + "=<seconds> to application.properties.");
        }
        return cache;
    }
}
```

---

## 9. `RedisCacheConfig.java` (rewrite in place)

Four changes against the current file: `implements CachingConfigurer` plus
`errorHandler()`, the `computePrefixWith` key prefix, `disableCreateOnMissingCache`,
and a `@ConfigurationProperties` record replacing the five `@Value` parameters.
`@ConditionalOnProperty` is removed: the `cache.enabled` flag now lives inside
the factory, so the `CacheManager` and `DomainCache` beans always exist and
constructor injection never breaks when the cache is off.

```java
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
        return new GenericJackson2JsonRedisSerializer();
    }
}
```

Verified behaviour on the versions in this project (Spring Framework 7.0.9,
Spring Data Redis 4.1.1):

- `CachingConfigurer.errorHandler()` returning a non-null handler is picked up by
  `CacheAspectSupport.configure(...)`; a null `cacheResolver()` falls back to
  `SimpleCacheResolver`, which resolves the single `CacheManager` bean in
  `afterSingletonsInstantiated()`.
- Only one `CachingConfigurer` bean is allowed in the context. This class is the only one.
- `entryTtl(Duration.ZERO)` maps to `TtlFunction.just(ZERO)` and
  `DefaultRedisCacheWriter.isPositiveDuration()` rejects it, so no `EXPIRE` is
  sent: `0` really means persistent.
- `NoOpCacheManager.getCache(name)` returns a non-null cache for any name, so
  `DomainCache.require` cannot fail when `cache.enabled=false`.

---

## 10. Service edits

Identical pattern in the five services. Two changes each: a `DomainCache`
constructor parameter, and `getAll()` rewritten.

### 10.1 Constructor

Add `DomainCache domainCache` as the last parameter, store it in a new
`private final DomainCache domainCache;` field, and add the import
`com.jerokysys.academic.config.DomainCache`.

| Service | Current constructor |
|---|---|
| `EnrollmentService` | `(EnrollmentRepo, StudentRepo, DanceClassRepo, EnrollmentMapper)` |
| `DanceClassService` | `(DanceClassRepo, DisciplineRepo, DanceClassMapper)` |
| `DisciplineService` | `(DisciplineRepo, DisciplineMapper)` |
| `ExamService` | `(ExamRepo, DanceClassRepo, ExamMapper)` |
| `ExamResultService` | `(ExamResultRepo, ExamRepo, StudentRepo, ExamResultMapper)` |

### 10.2 `getAll()`

Remove the `@Cacheable` annotation (requirement 9) and warm each element.

```java
@Override
public List<EnrollmentDTO> getAll() {
    List<EnrollmentDTO> list = super.getAll();
    domainCache.putAll(RedisCacheConfig.CACHE_ENROLLMENT, list, EnrollmentDTO::getId);
    return list;
}
```

Exact lines to replace, one per service:

| Service | Line to remove |
|---|---|
| `EnrollmentService` | `@Cacheable(value = RedisCacheConfig.CACHE_ENROLLMENT, key = "'enrollment:all'")` |
| `DanceClassService` | `@Cacheable(value = RedisCacheConfig.CACHE_DANCE_CLASS, key = "'danceclass:all'")` |
| `DisciplineService` | `@Cacheable(value = RedisCacheConfig.CACHE_DISCIPLINE, key = "'discipline:all'")` |
| `ExamService` | `@Cacheable(value = RedisCacheConfig.CACHE_EXAM, key = "'exam:all'")` |
| `ExamResultService` | `@Cacheable(value = RedisCacheConfig.CACHE_EXAM_RESULT, key = "'examresult:all'")` |

Every DTO exposes `Long getId()` (`EnrollmentDTO`, `DanceClassDTO`,
`DisciplineDTO`, `ExamDTO`, `ExamResultDTO`), so `DTO::getId` is the same id
source used by the SpEL keys.

### 10.3 Unchanged

- `@Cacheable(value = ..., key = "#id")` on `getById` — the key must change from
  `"'<domain>:' + #id"` to `"#id"`, because the domain now comes from the prefix.
  Five edits, one per service:

  | Service | `getById` key today | becomes |
  |---|---|---|
  | Enrollment | `"'enrollment:' + #id"` | `"#id"` |
  | DanceClass | `"'danceclass:' + #id"` | `"#id"` |
  | Discipline | `"'discipline:' + #id"` | `"#id"` |
  | Exam | `"'exam:' + #id"` | `"#id"` |
  | ExamResult | `"'examresult:' + #id"` | `"#id"` |

- `@CachePut(key = "'<domain>:' + #result.id")` on `create` → also becomes `"#result.id"`.
- `@CacheEvict(allEntries = true)` on `update` and `delete`.
- The `@CachePut` + `@CacheEvict(allEntries = true)` pair on `create`.
- Everything else: `search`, `getByFilter`, `getByStudent`, `getByClass`,
  `getByStatus`, `getByDate` get no annotation.

**Key consistency rule (R8):** the `@Cacheable` key, the `@CachePut` key and the
`DomainCache` key extractor must all produce the bare id. The prefix and the
domain are added once, by the manager. With that, read, write and list fill all
land on `academic:<domain>:<id>`.

---

## 11. `application.properties`

Replaces the block from `cache.enabled=true` through the five
`cache.<entity>.ttl-seconds` lines, and the two `spring.data.redis` lines.

```properties
# ------------
# Redis Cache
# ------------
# One Redis instance for this microservice. Every value comes from this file;
# the source code holds no cache literals.
cache.enabled=${CACHE_ENABLED:true}
cache.key-prefix=academic
cache.default-ttl-seconds=120
cache.scan-batch-size=100
# Domains (sections). Seconds; 0 = persistent, no expiration.
cache.domains.discipline=0
cache.domains.danceclass=120
cache.domains.enrollment=30
cache.domains.exam=30
cache.domains.examresult=30

spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
spring.data.redis.database=0
spring.data.redis.password=${REDIS_PASSWORD:}
spring.data.redis.connect-timeout=2s
spring.data.redis.timeout=2s

# One line per cache operation. Set to WARN to silence it.
logging.level.com.jerokysys.academic.config.LoggingCache=${CACHE_LOG_LEVEL:INFO}
```

### Retention matrix (R10)

| Domain | Objects | Policy | Property |
|---|---|---|---|
| `discipline` | `DisciplineDTO` | persistent | `cache.domains.discipline=0` |
| `danceclass` | `DanceClassDTO` | TTL 120 s | `cache.domains.danceclass=120` |
| `enrollment` | `EnrollmentDTO` | TTL 30 s | `cache.domains.enrollment=30` |
| `exam` | `ExamDTO` | TTL 30 s | `cache.domains.exam=30` |
| `examresult` | `ExamResultDTO` | TTL 30 s | `cache.domains.examresult=30` |

`discipline` is a catalog that changes only through this API, and every write
path clears its own section, so it never needs an expiry. Every mutable,
frequently written object gets a TTL.

### Server recommendation

The retention policy depends on how Redis evicts. Recommended settings for the
instance this service connects to:

```
maxmemory 256mb
maxmemory-policy volatile-lru
appendonly yes
```

`volatile-lru` evicts only keys that have a TTL, so the persistent
`discipline` domain is never dropped under memory pressure. With
`allkeys-lru` it would be.

---

## 12. README section

Append to `academic/README.md`:

```markdown
## Redis cache

The service caches DTOs in Redis to protect the database. Every microservice
runs its own Redis on `localhost:6379`, database `0`.

- Keys: `<key-prefix>:<domain>:<id>`, for example `academic:enrollment:5`
- Domains and TTLs: `cache.domains.*` in `src/main/resources/application.properties`
  (`0` = persistent, any other value = seconds)
- Reads by id go through `@Cacheable`, writes through `@CachePut` and `@CacheEvict`
- List and paginated results are never cached as a whole: each element is stored
  individually by `DomainCache`
- Every operation is logged as `Cache GET|SET|DELETE <key>` with HIT/MISS and TTL;
  the level is `logging.level.com.jerokysys.academic.config.LoggingCache`
- If Redis is unavailable, the request still succeeds against PostgreSQL and the
  failure is logged
```

---

## 13. Verification

Run in order. Redis must listen on `localhost:6379`.

1. Build: `./mvnw -q -DskipTests compile`
2. Start: `./mvnw spring-boot:run`. Startup must print one line per domain:
   ```
   Cache 'enrollment' -> keys academic:enrollment: (TTL 30s)
   Cache 'discipline' -> keys academic:discipline: (persistent)
   ```
3. Read the same resource twice:
   ```bash
   curl -s localhost:8082/enrollments/1
   curl -s localhost:8082/enrollments/1
   ```
   Log: `Cache GET academic:enrollment:1 -> MISS`, then `-> HIT`.
4. Key layout:
   ```bash
   redis-cli KEYS 'academic:*'
   ```
   Only `academic:<domain>:<id>` keys. No `academic:enrollment:all`.
5. `GET /enrollments/all` → one SQL query, then one `Cache SET
   academic:enrollment:<id>` line per element, no whole-list key.
6. `GET /enrollments?page=0` → SQL only, zero cache lines.
7. Create a resource → `Cache DELETE academic:enrollment:* (domain)` and a
   `Cache SET academic:enrollment:<new id> (TTL 30s)`.
8. Update and delete → `Cache DELETE academic:enrollment:* (domain)`.
9. Read `GET /disciplines/1` twice → the second read must be `HIT`.
10. Redis outage:
    ```bash
    redis-cli shutdown nosave
    curl -i localhost:8082/enrollments/1     # still 200
    grep "Cache unavailable" app.log
    redis-server --daemonize yes
    ```
11. Inspect: `grep "Cache " app.log | tail -30`

---

## 14. Acceptance criteria

| # | Requirement | Evidence |
|---|---|---|
| R1 | Cache on the API side | Annotations in `services/`, `CacheManager` in `config/`. No cache outside `academic` |
| R2 | One Redis per microservice | Own `spring.data.redis.*` block, `localhost:6379`, `database=0` |
| R3 | All objects managed | `@Cacheable` on read, `@CachePut` on write, `DomainCache.putAll` on list reads |
| R4 | Domains | One cache per `cache.domains.*` line, `evict` touches only its own section |
| R5 | Annotations + programmatic | Annotations for CRUD, `DomainCache` calls `cacheManager.getCache` |
| R6 | Logging | `LoggingCache` logs get/set/putIfAbsent/evict/clear with the real key, HIT/MISS and TTL; level in properties |
| R7 | External configuration | 10 properties, no literal in Java; an undeclared domain fails at startup |
| R8 | Key strategy | `academic:<domain>:<id>`, same id source on read, write and fill |
| R9 | No lists or pages | No `@Cacheable` on `getAll`, no annotation on any list query, elements cached one key each |
| R10 | Retention | Explicit matrix, `0` = persistent, `>0` = TTL, printed at startup |

Build must pass, and step 3, 5, 6 and 10 of section 13 must all hold.
