package com.jerokysys.academic.config;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
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