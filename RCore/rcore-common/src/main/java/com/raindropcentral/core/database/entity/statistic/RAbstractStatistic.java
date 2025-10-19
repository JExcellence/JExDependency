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
 * inheritance. Subclasses specialize the {@code statistic_type} discriminator to map different
 * data representations while sharing identifier and plugin metadata.
 *
 * <p>The unique constraint on (identifier, player_statistic_id) prevents duplicate statistic
 * keys per player aggregate. Instances should be created and mutated within repository-managed
 * transactions to ensure Hibernate change tracking remains consistent.</p>
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
     * Column mapping for the logical statistic identifier. Combined with
     * {@code player_statistic_id} to enforce per-player uniqueness.
     */
    @Column(name = "identifier", nullable = false, length = 100)
    protected String identifier;

    /**
     * Column mapping for the owning plugin namespace. Used to partition statistics across
     * modules and enable targeted clean-up routines.
     */
    @Column(name = "plugin", nullable = false, length = 50)
    private String plugin;

    /**
     * Owning aggregate relation configured through {@code player_statistic_id}. Marked lazy to
     * reduce fetch overhead when materializing statistics independently.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_statistic_id")
    private RPlayerStatistic playerStatistic;

    /**
     * Default constructor for Hibernate proxying.
     */
    protected RAbstractStatistic() {}

    /**
     * Creates a statistic with the required identifier and plugin metadata.
     *
     * @param identifier unique statistic identifier within a player aggregate
     * @param plugin     plugin namespace responsible for the statistic
     */
    protected RAbstractStatistic(final @NotNull String identifier, final @NotNull String plugin) {
        this.identifier = Objects.requireNonNull(identifier, "identifier cannot be null");
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
    }

    /**
     * Retrieves the stored statistic value. Implementations must return immutable or cloned
     * data when exposing mutable types to avoid shared state across threads.
     *
     * @return concrete statistic value
     */
    public abstract @NotNull Object getValue();

    /**
     * @return logical identifier persisted in {@code identifier}
     */
    public @NotNull String getIdentifier() {
        return this.identifier;
    }

    /**
     * @return plugin namespace persisted in {@code plugin}
     */
    public @NotNull String getPlugin() {
        return this.plugin;
    }

    /**
     * Provides the owning aggregate, which may be {@code null} when the entity is detached
     * or prior to association.
     *
     * @return owning {@link RPlayerStatistic} aggregate or {@code null}
     */
    public @Nullable RPlayerStatistic getPlayerStatistic() {
        return this.playerStatistic;
    }

    /**
     * Sets the owning aggregate reference. Typically invoked by aggregate helper methods to
     * maintain bidirectional consistency.
     *
     * @param playerStatistic aggregate to associate, may be {@code null} when detaching
     */
    public void setPlayerStatistic(final @Nullable RPlayerStatistic playerStatistic) {
        this.playerStatistic = playerStatistic;
    }

    /**
     * Checks whether this statistic matches the provided identifier and plugin pair.
     *
     * @param identifier identifier to test
     * @param plugin     plugin namespace to test
     * @return {@code true} when both identifier and plugin equal this statistic's values
     */
    public boolean matches(final @NotNull String identifier, final @NotNull String plugin) {
        return this.identifier.equals(identifier) && this.plugin.equals(plugin);
    }
}
