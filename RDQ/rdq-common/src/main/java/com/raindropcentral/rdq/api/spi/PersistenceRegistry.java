package com.raindropcentral.rdq.api.spi;

import java.util.Optional;

/**
 * Aggregates optional SPI adapters that RDQ editions can contribute during bootstrap.
 *
 * <p><strong>Threading:</strong> The registry is immutable after construction and therefore thread
 * safe. Callers may query from any RDQ executor.</p>
 *
 * <p><strong>Lifecycle:</strong> Instances are created once per edition during repository wiring and
 * shared for the module lifetime.</p>
 *
 * <p><strong>Integration:</strong> Provides a centralized lookup for edition-specific persistence
 * adapters used by gameplay services, administrative tooling, and background synchronizers.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class PersistenceRegistry {
    /**
     * Adapter that persists {@link com.raindropcentral.rdq.database.entity.bounty.RBounty}
     * instances when editions provide a custom implementation.
     *
     * <p><strong>Nullability:</strong> May be {@code null} when editions rely on default behaviour.</p>
     */
    private final BountyPersistence bountyPersistence;

    /**
     * Adapter responsible for synchronising {@link com.raindropcentral.rdq.database.entity.player.RDQPlayer}
     * state to external storage when available.
     *
     * <p><strong>Nullability:</strong> May be {@code null} when no adapter is provided.</p>
     */
    private final PlayerPersistence playerPersistence;

    /**
     * Creates a registry from the supplied builder.
     *
     * @param builder the builder carrying configured adapters
     */
    protected PersistenceRegistry(Builder builder) {
        this.bountyPersistence = builder.bountyPersistence;
        this.playerPersistence = builder.playerPersistence;
    }

    /**
     * Creates a new builder for assembling registry instances.
     *
     * @return a builder with no adapters configured
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Obtains the registered {@link BountyPersistence} adapter, if present.
     *
     * @return an {@link Optional} describing the configured adapter
     */
    public Optional<BountyPersistence> getBountyPersistence() {
        return Optional.ofNullable(bountyPersistence);
    }

    /**
     * Obtains the registered {@link PlayerPersistence} adapter, if present.
     *
     * @return an {@link Optional} describing the configured adapter
     */
    public Optional<PlayerPersistence> getPlayerPersistence() {
        return Optional.ofNullable(playerPersistence);
    }

    /**
     * Builder for {@link PersistenceRegistry} instances.
     */
    public static final class Builder {
        /**
         * Candidate adapter for handling bounty persistence operations.
         *
         * <p><strong>Nullability:</strong> Optional; {@code null} indicates no adapter.</p>
         */
        private BountyPersistence bountyPersistence;

        /**
         * Candidate adapter for handling player persistence operations.
         *
         * <p><strong>Nullability:</strong> Optional; {@code null} indicates no adapter.</p>
         */
        private PlayerPersistence playerPersistence;

        /**
         * Registers the bounty persistence adapter that RDQ should use.
         *
         * @param bountyPersistence the adapter implementation; {@code null} clears the assignment
         * @return this builder for chaining
         */
        public Builder bountyPersistence(BountyPersistence bountyPersistence) {
            this.bountyPersistence = bountyPersistence;
            return this;
        }

        /**
         * Registers the player persistence adapter that RDQ should use.
         *
         * @param playerPersistence the adapter implementation; {@code null} clears the assignment
         * @return this builder for chaining
         */
        public Builder playerPersistence(PlayerPersistence playerPersistence) {
            this.playerPersistence = playerPersistence;
            return this;
        }

        /**
         * Builds an immutable registry snapshot with the currently registered adapters.
         *
         * @return a new {@link PersistenceRegistry}
         */
        public PersistenceRegistry build() {
            return new PersistenceRegistry(this);
        }
    }
}