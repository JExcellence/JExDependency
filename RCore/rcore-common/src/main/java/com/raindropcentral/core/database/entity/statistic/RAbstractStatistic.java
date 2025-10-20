package com.raindropcentral.core.database.entity.statistic;

import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.Objects;

/**
 * Base class for persisted statistic values stored in {@code r_statistic} via single-table
 * inheritance. Each subclass selects a discriminator value stored in the
 * {@code statistic_type} column, enabling Hibernate to materialize concrete implementations
 * while sharing identifier, plugin, and association management concerns.
 *
 * <p>The table-level unique constraint on {@code (identifier, player_statistic_id)} guarantees
 * that a player aggregate cannot persist two statistics with the same logical key. Entities are
 * expected to be created, mutated, and flushed within repository-managed transactions so that
 * discriminator updates and dirty-checking remain synchronized.</p>
 *
 * <p>The {@link RPlayerStatistic} association is maintained on the statistic side via the
 * {@code player_statistic_id} foreign key. It is intentionally configured for lazy fetching to
 * avoid loading the owning aggregate when querying statistics in isolation. Callers updating
 * bidirectional links must use {@link #setPlayerStatistic(RPlayerStatistic)} (and the
 * corresponding helper on {@link RPlayerStatistic}) to keep both sides aligned.</p>
 * <p>
 * Subclasses should log significant lifecycle events through
 * {@link com.raindropcentral.rplatform.logging.CentralLogger CentralLogger}: attaching or detaching
 * from an aggregate, mutating a value, or encountering validation failures. Value mutations should
 * be announced prior to committing so audit trails can reconstruct progression, while errors should
 * log at the appropriate severity and include both the statistic identifier and owning player keys.
 * </p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "statistic_type")
@Table(
    name = "r_statistic",
    uniqueConstraints = @UniqueConstraint(columnNames = {"identifier", "player_statistic_id"})
)
public abstract class RAbstractStatistic extends AbstractEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Column mapping for the logical statistic identifier. Combined with the
     * {@code player_statistic_id} foreign key to enforce per-player uniqueness and power the
     * {@link #matches(String, String)} predicate.
     */
    @Column(name = "identifier", nullable = false, length = 100)
    protected String identifier;

    /**
     * Column mapping for the owning plugin namespace. The namespace partitions statistics across
     * modules, allowing scheduled clean-up routines to scope deletions or recalculations to a
     * specific provider.
     */
    @Column(name = "plugin", nullable = false, length = 50)
    private String plugin;

    /**
     * Owning aggregate relation configured through {@code player_statistic_id}. The association is
     * lazy to reduce fetch overhead when materializing statistics independently, and should be set
     * alongside the inverse collection on {@link RPlayerStatistic} to maintain consistency.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_statistic_id")
    private RPlayerStatistic playerStatistic;

    /**
     * Default constructor reserved for Hibernate proxying and reflective instantiation.
     */
    protected RAbstractStatistic() {}

    /**
     * Creates a statistic with the required identifier and plugin metadata. Subclasses are
     * expected to populate their discriminator-specific state immediately after invoking this
     * constructor.
     *
     * @param identifier unique statistic identifier within a player aggregate, never {@code null}
     * @param plugin     plugin namespace responsible for the statistic, never {@code null}
     */
    protected RAbstractStatistic(final @NotNull String identifier, final @NotNull String plugin) {
        this.identifier = Objects.requireNonNull(identifier, "identifier cannot be null");
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
    }

    /**
     * Retrieves the stored statistic value. Implementations must return immutable data or a
     * defensive copy when exposing mutable types to avoid shared state across transactions or
     * threads.
     *
     * @return concrete statistic value represented by the subclass
     */
    public abstract @NotNull Object getValue();

    /**
     * Obtains the logical identifier persisted in the {@code identifier} column.
     *
     * @return logical identifier persisted in {@code identifier}
     */
    public @NotNull String getIdentifier() {
        return this.identifier;
    }

    /**
     * Provides the plugin namespace responsible for calculating this statistic.
     *
     * @return plugin namespace persisted in {@code plugin}
     */
    public @NotNull String getPlugin() {
        return this.plugin;
    }

    /**
     * Provides the owning aggregate, which may be {@code null} when the entity is detached,
     * prior to association, or has been intentionally orphaned for reassignment.
     *
     * @return owning {@link RPlayerStatistic} aggregate or {@code null}
     */
    public @Nullable RPlayerStatistic getPlayerStatistic() {
        return this.playerStatistic;
    }

    /**
     * Sets the owning aggregate reference. Typically invoked by aggregate helper methods to
     * maintain bidirectional consistency and enforce the unique constraint described in the class
     * documentation.
     *
     * @param playerStatistic aggregate to associate, may be {@code null} when detaching
     */
    public void setPlayerStatistic(final @Nullable RPlayerStatistic playerStatistic) {
        this.playerStatistic = playerStatistic;
    }

    /**
     * Checks whether this statistic matches the provided identifier and plugin pair. Both
     * arguments are required to be non-null and correspond to the same semantics as
     * {@link #getIdentifier()} and {@link #getPlugin()}.
     *
     * @param identifier identifier to test, never {@code null}
     * @param plugin     plugin namespace to test, never {@code null}
     * @return {@code true} when both identifier and plugin equal this statistic's values
     */
    public boolean matches(final @NotNull String identifier, final @NotNull String plugin) {
        return this.identifier.equals(identifier) && this.plugin.equals(plugin);
    }
}
