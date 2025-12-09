package com.raindropcentral.rdq2.perk.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class PerkRuntimeStateService {

    private final ConcurrentMap<String, PerkRuntimeState> stateByPerk = new ConcurrentHashMap<>();

    public @NotNull PerkRuntimeState stateFor(@NotNull String perkId) {
        Objects.requireNonNull(perkId, "perkId");
        return stateByPerk.computeIfAbsent(perkId, PerkRuntimeState::new);
    }

    public void clearPlayer(@NotNull UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        stateByPerk.values().forEach(state -> state.clearPlayer(playerId));
    }

    public void clearPerk(@NotNull String perkId) {
        Objects.requireNonNull(perkId, "perkId");
        var state = stateByPerk.remove(perkId);
        if (state != null) state.clearAll();
    }

    public void clearAll() {
        stateByPerk.values().forEach(PerkRuntimeState::clearAll);
        stateByPerk.clear();
    }

    public static final class PerkRuntimeState {

        private final String perkId;
        private final ConcurrentMap<UUID, RuntimeEntry> activePlayers = new ConcurrentHashMap<>();
        private final AtomicInteger activeCount = new AtomicInteger(0);

        private PerkRuntimeState(@NotNull String perkId) {
            this.perkId = perkId;
        }

        public boolean markActive(@NotNull UUID playerId, long expiryMillis, @Nullable Integer maxConcurrentUsers) {
            Objects.requireNonNull(playerId, "playerId");
            var allowed = new AtomicReference<>(Boolean.TRUE);
            activePlayers.compute(playerId, (id, existing) -> {
                if (existing != null) return existing.withExpiry(expiryMillis);
                
                if (maxConcurrentUsers != null) {
                    while (true) {
                        var current = activeCount.get();
                        if (current >= maxConcurrentUsers) {
                            allowed.set(Boolean.FALSE);
                            return null;
                        }
                        if (activeCount.compareAndSet(current, current + 1)) break;
                    }
                } else {
                    activeCount.incrementAndGet();
                }
                return new RuntimeEntry(expiryMillis);
            });
            return Boolean.TRUE.equals(allowed.get()) && activePlayers.containsKey(playerId);
        }

        public void markInactive(@NotNull UUID playerId) {
            Objects.requireNonNull(playerId, "playerId");
            activePlayers.computeIfPresent(playerId, (id, entry) -> {
                activeCount.decrementAndGet();
                return null;
            });
        }

        public boolean isActive(@NotNull UUID playerId, @NotNull Runnable onExpired) {
            Objects.requireNonNull(playerId, "playerId");
            Objects.requireNonNull(onExpired, "onExpired");
            var entry = activePlayers.get(playerId);
            if (entry == null) return false;
            
            if (entry.isExpired()) {
                if (activePlayers.remove(playerId, entry)) {
                    activeCount.decrementAndGet();
                    onExpired.run();
                }
                return false;
            }
            return true;
        }

        /**
         * Iterates over all players currently tracked as active. Expired entries are
         * pruned automatically before invocation.
         *
         * @param action the consumer invoked for every active player identifier
         */
        public void forEachActive(@NotNull Consumer<UUID> action) {
            Objects.requireNonNull(action, "action");
            final Set<UUID> snapshot = Set.copyOf(activePlayers.keySet());
            for (UUID playerId : snapshot) {
                final RuntimeEntry entry = activePlayers.get(playerId);
                if (entry == null) {
                    continue;
                }
                if (entry.isExpired()) {
                    if (activePlayers.remove(playerId, entry)) {
                        activeCount.decrementAndGet();
                    }
                    continue;
                }
                action.accept(playerId);
            }
        }

        /**
         * Provides the number of players currently marked as active, excluding expired
         * activations.
         *
         * @return the active player count
         */
        public int activeCount() {
            pruneExpired();
            return activeCount.get();
        }

        /**
         * Clears the tracked state for the supplied player identifier.
         *
         * @param playerId the player identifier to clear
         */
        public void clearPlayer(@NotNull UUID playerId) {
            markInactive(playerId);
        }

        /**
         * Removes every tracked activation for this perk.
         */
        public void clearAll() {
            activePlayers.clear();
            activeCount.set(0);
        }

        private void pruneExpired() {
            final long now = System.currentTimeMillis();
            final Set<UUID> snapshot = Set.copyOf(activePlayers.keySet());
            for (UUID playerId : snapshot) {
                activePlayers.computeIfPresent(playerId, (id, entry) -> {
                    if (entry.isExpired(now)) {
                        activeCount.decrementAndGet();
                        return null;
                    }
                    return entry;
                });
            }
        }

        private record RuntimeEntry(long expiryMillis) {

            private RuntimeEntry withExpiry(long newExpiryMillis) {
                return new RuntimeEntry(Math.max(0L, newExpiryMillis));
            }

            private boolean isExpired() {
                return isExpired(System.currentTimeMillis());
            }

            private boolean isExpired(long now) {
                return expiryMillis > 0 && expiryMillis <= now;
            }
        }

        @Override
        public String toString() {
            return "PerkRuntimeState{" +
                "perkId='" + perkId + '\'' +
                ", activeCount=" + activeCount +
                '}';
        }
    }
}
