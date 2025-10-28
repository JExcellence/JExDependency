package com.raindropcentral.rdq.perk.runtime;

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

/**
 * Centralized store for tracking runtime perk activation state across all players.
 * <p>
 * The legacy implementation embedded state directly within each {@link PerkRuntime}
 * instance, which made it impossible to release resources when a player disconnected
 * or when a perk was reloaded. This service provides a shared view that runtimes can
 * use to coordinate activation limits, durations, and cleanup.
 * </p>
 *
 * <p>All methods are thread-safe and rely on lock-free data structures to minimise
 * the impact on the main server thread.</p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 3.2.0
 */
public final class PerkRuntimeStateService {

    private final ConcurrentMap<String, PerkRuntimeState> stateByPerk = new ConcurrentHashMap<>();

    /**
     * Gets (or creates) the state container for the specified perk identifier.
     *
     * @param perkId the unique perk identifier
     * @return a non-{@code null} state container bound to the perk
     */
    public @NotNull PerkRuntimeState stateFor(@NotNull String perkId) {
        Objects.requireNonNull(perkId, "perkId");
        return stateByPerk.computeIfAbsent(perkId, PerkRuntimeState::new);
    }

    /**
     * Clears all activation state tracked for the provided player.
     *
     * @param playerId the player identifier to purge
     */
    public void clearPlayer(@NotNull UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        stateByPerk.values().forEach(state -> state.clearPlayer(playerId));
    }

    /**
     * Clears all activation state for the supplied perk identifier.
     *
     * @param perkId the perk identifier to purge
     */
    public void clearPerk(@NotNull String perkId) {
        Objects.requireNonNull(perkId, "perkId");
        final PerkRuntimeState state = stateByPerk.remove(perkId);
        if (state != null) {
            state.clearAll();
        }
    }

    /**
     * Removes every tracked activation across all perks and players.
     */
    public void clearAll() {
        stateByPerk.values().forEach(PerkRuntimeState::clearAll);
        stateByPerk.clear();
    }

    /**
     * Represents the activation state for a single perk, exposing utilities to
     * manage concurrent access and duration tracking.
     */
    public static final class PerkRuntimeState {

        private final String perkId;
        private final ConcurrentMap<UUID, RuntimeEntry> activePlayers = new ConcurrentHashMap<>();
        private final AtomicInteger activeCount = new AtomicInteger(0);

        private PerkRuntimeState(@NotNull String perkId) {
            this.perkId = perkId;
        }

        /**
         * Attempts to mark a player as active for this perk, enforcing the optional
         * concurrent user limit. When the player is already active their expiry is
         * updated without affecting the active counter.
         *
         * @param playerId the player identifier
         * @param expiryMillis the activation expiry in milliseconds (0 for indefinite)
         * @param maxConcurrentUsers optional concurrency limit; {@code null} disables the check
         * @return {@code true} when the activation slot has been reserved, {@code false} if the limit was reached
         */
        public boolean markActive(@NotNull UUID playerId, long expiryMillis, @Nullable Integer maxConcurrentUsers) {
            Objects.requireNonNull(playerId, "playerId");

            // If the player is already active, just update the expiry.
            RuntimeEntry existingEntry = activePlayers.get(playerId);
            if (existingEntry != null) {
                activePlayers.put(playerId, existingEntry.withExpiry(expiryMillis));
                return true;
            }

            // For new activations, check the concurrency limit first.
            if (maxConcurrentUsers != null) {
                if (activeCount.get() >= maxConcurrentUsers) {
                    return false;
                }
            }

            final AtomicReference<Boolean> success = new AtomicReference<>(false);
            activePlayers.computeIfAbsent(playerId, id -> {
                if (maxConcurrentUsers != null) {
                    // Double-check limit inside compute to handle concurrent activations
                    while (true) {
                        int current = activeCount.get();
                        if (current >= maxConcurrentUsers) {
                            return null; // Another thread took the last spot
                        }
                        if (activeCount.compareAndSet(current, current + 1)) {
                            success.set(true);
                            return new RuntimeEntry(expiryMillis);
                        }
                    }
                } else {
                    activeCount.incrementAndGet();
                    success.set(true);
                    return new RuntimeEntry(expiryMillis);
                }
            });

            return success.get();
        }

        /**
         * Marks the supplied player as inactive, freeing any reserved slot.
         *
         * @param playerId the player identifier
         */
        public void markInactive(@NotNull UUID playerId) {
            Objects.requireNonNull(playerId, "playerId");
            activePlayers.computeIfPresent(playerId, (id, entry) -> {
                activeCount.decrementAndGet();
                return null;
            });
        }

        /**
         * Checks whether the player is currently marked as active. When an activation
         * has expired the supplied callback is invoked so the caller can perform any
         * required cleanup (e.g., deactivating potion effects).
         *
         * @param playerId the player identifier
         * @param onExpired callback executed exactly once when the activation expires
         * @return {@code true} if the player is still active, {@code false} otherwise
         */
        public boolean isActive(@NotNull UUID playerId, @NotNull Runnable onExpired) {
            Objects.requireNonNull(playerId, "playerId");
            Objects.requireNonNull(onExpired, "onExpired");
            final RuntimeEntry entry = activePlayers.get(playerId);
            if (entry == null) {
                return false;
            }
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
