package com.trizen.photoshare.service;

import com.trizen.photoshare.config.AppProperties;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Throttles PIN guessing. A 6 digit PIN is only safe if an attacker cannot try
 * a million of them, so failures are counted per gallery and client and the
 * gallery locks for a cooldown window.
 *
 * In memory on purpose: it is per node and resets on restart. A multi node
 * deployment would move this to Redis, which is noted in the README.
 */
@Service
public class PinAttemptService {

    private record Attempts(int count, Instant firstFailureAt, Instant lockedUntil) {
    }

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final AppProperties properties;

    public PinAttemptService(AppProperties properties) {
        this.properties = properties;
    }

    public boolean isLocked(String slug, String client) {
        Attempts current = attempts.get(key(slug, client));
        return current != null && current.lockedUntil() != null
                && Instant.now().isBefore(current.lockedUntil());
    }

    public long secondsUntilUnlock(String slug, String client) {
        Attempts current = attempts.get(key(slug, client));
        if (current == null || current.lockedUntil() == null) {
            return 0;
        }
        return Math.max(0, current.lockedUntil().getEpochSecond() - Instant.now().getEpochSecond());
    }

    public int remainingAttempts(String slug, String client) {
        Attempts current = attempts.get(key(slug, client));
        int used = current == null ? 0 : current.count();
        return Math.max(0, properties.getGallery().getMaxPinAttempts() - used);
    }

    public void recordFailure(String slug, String client) {
        attempts.compute(key(slug, client), (k, current) -> {
            int count = current == null ? 1 : current.count() + 1;
            Instant first = current == null ? Instant.now() : current.firstFailureAt();
            Instant lockedUntil = null;
            if (count >= properties.getGallery().getMaxPinAttempts()) {
                lockedUntil = Instant.now()
                        .plusSeconds(properties.getGallery().getPinLockoutMinutes() * 60);
            }
            return new Attempts(count, first, lockedUntil);
        });
    }

    public void recordSuccess(String slug, String client) {
        attempts.remove(key(slug, client));
    }

    private String key(String slug, String client) {
        return slug + "|" + client;
    }
}
