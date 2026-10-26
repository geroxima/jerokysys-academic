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