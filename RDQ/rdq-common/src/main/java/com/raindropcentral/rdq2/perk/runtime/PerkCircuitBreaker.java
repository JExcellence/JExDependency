package com.raindropcentral.rdq2.perk.runtime;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Provides circuit-breaking semantics for perk runtimes by tracking short term failure windows
 * and suspending players when repeated errors occur.
 *
 * <p>The class is intentionally lightweight so it can operate comfortably on the main server
 * thread while remaining safe for asynchronous access. Internally it utilises lock-free
 * structures, guaranteeing that suspension updates do not block other perk operations.</p>
 *
 * @author JExcellence
 * @since 3.2.0
 * @version 1.0.0
 */
public final class PerkCircuitBreaker {

    private final long failureWindowMillis;
    private final int failureThreshold;
    private final long suspendDurationMillis;
    private final ConcurrentMap<UUID, FailureState> states = new ConcurrentHashMap<>();

    public PerkCircuitBreaker(long failureWindowMillis, int failureThreshold, long suspendDurationMillis) {
        this.failureWindowMillis = failureWindowMillis;
        this.failureThreshold = failureThreshold;
        this.suspendDurationMillis = suspendDurationMillis;
    }

    /**
     * Records a failure for the supplied player. When the failure threshold is reached the player
     * is suspended and the suspension duration is returned in seconds.
     *
     * @param playerId the player identifier
     * @return suspension duration in seconds, or {@code 0} if no suspension was applied
     */
    public long registerFailure(@NotNull UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        final long now = System.currentTimeMillis();
        final FailureState state = states.computeIfAbsent(playerId, ignored -> new FailureState());
        final long windowStart = state.windowStart.get();
        if (windowStart == 0L || (now - windowStart) > failureWindowMillis) {
            state.windowStart.set(now);
            state.failureCount.set(0L);
        }
        final long failures = state.failureCount.incrementAndGet();
        if (failures >= failureThreshold) {
            state.failureCount.set(0L);
            state.windowStart.set(now);
            final long suspendUntil = now + suspendDurationMillis;
            state.suspendedUntil.set(suspendUntil);
            return Math.max(1L, suspendDurationMillis / 1000L);
        }
        return 0L;
    }

    /**
     * Checks whether the supplied player is currently suspended. Expired suspensions are removed
     * transparently.
     *
     * @param playerId the player identifier
     * @return {@code true} if the player remains suspended
     */
    public boolean isSuspended(@NotNull UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        final FailureState state = states.get(playerId);
        if (state == null) {
            return false;
        }
        final long until = state.suspendedUntil.get();
        if (until <= 0L) {
            return false;
        }
        final long now = System.currentTimeMillis();
        if (until > now) {
            return true;
        }
        if (state.suspendedUntil.compareAndSet(until, 0L)) {
            state.failureCount.set(0L);
            state.windowStart.set(0L);
            states.remove(playerId, state);
        }
        return false;
    }

    /**
     * Provides the remaining suspension duration in seconds for the supplied player.
     *
     * @param playerId the player identifier
     * @return remaining suspension duration in seconds
     */
    public long getRemainingSuspensionSeconds(@NotNull UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        final FailureState state = states.get(playerId);
        if (state == null) {
            return 0L;
        }
        final long until = state.suspendedUntil.get();
        final long now = System.currentTimeMillis();
        if (until <= now) {
            return 0L;
        }
        return Math.max(0L, (until - now) / 1000L);
    }

    /**
     * Clears any recorded failures for the supplied player.
     *
     * @param playerId the player identifier
     */
    public void clear(@NotNull UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        final FailureState state = states.remove(playerId);
        if (state != null) {
            state.failureCount.set(0L);
            state.windowStart.set(0L);
            state.suspendedUntil.set(0L);
        }
    }

    /**
     * Removes all tracked failure states. Primarily used when perks are reloaded.
     */
    public void clearAll() {
        states.clear();
    }

    private static final class FailureState {

        private final AtomicLong windowStart = new AtomicLong();
        private final AtomicLong failureCount = new AtomicLong();
        private final AtomicLong suspendedUntil = new AtomicLong();
    }
}
