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
 * <p><strong>Usage:</strong> Editions obtain a builder through {@link #builder()}, register any
 * adapters that should override the built-in defaults, and call {@link Builder#build()} to produce
 * an immutable snapshot. Downstream services only rely on the {@link Optional} wrappers returned by
 * the getters and never dereference {@code null} adapters directly.</p>
 *
 * <p><strong>Extension guidance:</strong> When new persistence facets are introduced, extend the
 * builder and registry in tandem so all adapters remain discoverable from a single access point.
 * Keep the constructor package-private or protected to encourage usage of the builder which
 * performs any future validation.</p>
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
     * <p><strong>Validation:</strong> Currently performs direct assignment, but future releases may
     * vet adapter compatibility or apply defaults. Keep constructor invocations confined to the
     * {@link Builder} so those checks execute consistently.</p>
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
     * <p><strong>Threading:</strong> Builders are not thread-safe. Create, configure, and build them
     * on a single thread during RDQ bootstrap.</p>
     *
     * <p><strong>Usage:</strong> Editions call this factory, register adapters via the fluent setter
     * methods, and finish with {@link Builder#build()} once all SPI contributions are known.</p>
     *
     * @return a builder with no adapters configured
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Obtains the registered {@link BountyPersistence} adapter, if present.
     *
     * <p><strong>Null-safety:</strong> Wraps the nullable field in {@link Optional} so callers must
     * explicitly handle the absence of a custom adapter. When empty, RDQ falls back to the default
     * persistence implementation bundled with the edition.</p>
     *
     * <p><strong>Usage:</strong> Consumers should prefer the returned {@link Optional#orElseGet}
     * semantics to lazily initialize defaults when necessary.</p>
     *
     * @return an {@link Optional} describing the configured adapter
     */
    public Optional<BountyPersistence> getBountyPersistence() {
        return Optional.ofNullable(bountyPersistence);
    }

    /**
     * Obtains the registered {@link PlayerPersistence} adapter, if present.
     *
     * <p><strong>Null-safety:</strong> Wraps the nullable field in {@link Optional} so callers can
     * short-circuit to default behaviours without dereferencing {@code null}.</p>
     *
     * <p><strong>Usage:</strong> Editions may use {@link Optional#ifPresent(java.util.function.Consumer)}
     * to invoke custom persistence while retaining compatibility with stock implementations.</p>
     *
     * @return an {@link Optional} describing the configured adapter
     */
    public Optional<PlayerPersistence> getPlayerPersistence() {
        return Optional.ofNullable(playerPersistence);
    }

    /**
     * Builder for {@link PersistenceRegistry} instances.
     *
     * <p><strong>Threading:</strong> Mutable and therefore not thread-safe. Confine usage to a single
     * bootstrap thread.</p>
     *
     * <p><strong>Lifecycle:</strong> Intended for short-lived configuration during module wiring. The
     * resulting registry should be retained instead of the builder itself.</p>
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
         * <p><strong>Validation:</strong> Callers should only provide adapters that obey the
         * contracts defined by {@link BountyPersistence}. Future versions may enforce those
         * preconditions directly within the builder.</p>
         *
         * <p><strong>Usage:</strong> Passing {@code null} clears the assignment, allowing editions to
         * explicitly opt back into default behaviour.</p>
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
         * <p><strong>Validation:</strong> Adapters must satisfy the requirements documented on
         * {@link PlayerPersistence}. This method does not clone instances, so callers should avoid
         * reusing stateful adapters across multiple registries.</p>
         *
         * <p><strong>Usage:</strong> Supplying {@code null} clears any previously configured adapter.
         * This is useful when editions compose multiple registry contributions conditionally.</p>
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
         * <p><strong>Immutability:</strong> The produced registry copies the builder state and is safe
         * to share across threads once constructed.</p>
         *
         * <p><strong>Validation:</strong> Future enhancements may reject incompatible adapter
         * combinations here. Always prefer this factory to constructing registries manually.</p>
         *
         * @return a new {@link PersistenceRegistry}
         */
        public PersistenceRegistry build() {
            return new PersistenceRegistry(this);
        }
    }
}