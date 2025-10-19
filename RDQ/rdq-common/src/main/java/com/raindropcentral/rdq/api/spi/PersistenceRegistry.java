package com.raindropcentral.rdq.api.spi;

import java.util.Optional;

/**
 * Aggregates optional SPI adapters that RDQ editions can contribute during bootstrap.
 *
 * <p>The registry provides a type-safe way to supply persistence implementations without
 * forcing every edition to ship all adapters. Consumers should query the registry before
 * attempting persistence operations and fall back to default strategies when an adapter is
 * absent.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class PersistenceRegistry {
    /**
     * Adapter that persists {@link com.raindropcentral.rdq.database.entity.bounty.RBounty}
     * instances when editions provide a custom implementation.
     */
    private final BountyPersistence bountyPersistence;

    /**
     * Adapter responsible for synchronising {@link com.raindropcentral.rdq.database.entity.player.RDQPlayer}
     * state to external storage when available.
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
         */
        private BountyPersistence bountyPersistence;

        /**
         * Candidate adapter for handling player persistence operations.
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