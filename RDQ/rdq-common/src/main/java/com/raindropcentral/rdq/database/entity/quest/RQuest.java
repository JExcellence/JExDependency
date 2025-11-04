package com.raindropcentral.rdq.database.entity.quest;

import com.raindropcentral.rplatform.database.converter.BasicMaterialConverter;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents the shared state and behavior for all quest definitions stored in the
 * database. Concrete implementations provide the upgrade metadata and showcase item used
 * within quest menus.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@Entity
public abstract class RQuest extends AbstractEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "identifier", unique = true, nullable = false)
    private String identifier;

    @Column(name = "initial_upgrade_level", nullable = false)
    private int initialUpgradeLevel;

    @Column(name = "maximum_upgrade_level", nullable = false)
    private int maximumUpgradeLevel;

    @Column(name = "showcase_item", nullable = false, columnDefinition = "LONGTEXT")
    @Convert(converter = BasicMaterialConverter.class)
    private Material showcase;

    @OneToMany(mappedBy = "quest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RQuestUpgrade> upgrades = new ArrayList<>();

    protected RQuest() {}

    /**
     * Creates a quest definition with the supplied identifier and upgrade range.
     *
     * @param identifier the unique quest identifier.
     * @param initialUpgradeLevel the level a quest starts at when unlocked.
     * @param maximumUpgradeLevel the highest level the quest may reach.
     */
    public RQuest(final @NotNull String identifier, final int initialUpgradeLevel, final int maximumUpgradeLevel) {
        this.identifier = Objects.requireNonNull(identifier, "identifier cannot be null");
        this.initialUpgradeLevel = initialUpgradeLevel;
        this.maximumUpgradeLevel = maximumUpgradeLevel;
        this.showcase = this.initializeShowcase();
    }

    /**
     * Retrieves the quest identifier.
     *
     * @return a unique quest identifier string.
     */
    public @NotNull String getIdentifier() {
        return this.identifier;
    }

    /**
     * Obtains the initial upgrade level a quest starts at.
     *
     * @return the initial quest upgrade level.
     */
    public int getInitialUpgradeLevel() {
        return this.initialUpgradeLevel;
    }

    /**
     * Obtains the maximum upgrade level the quest can reach.
     *
     * @return the maximum quest upgrade level.
     */
    public int getMaximumUpgradeLevel() {
        return this.maximumUpgradeLevel;
    }

    /**
     * Retrieves the showcase material used when displaying the quest in menus.
     *
     * @return the configured showcase material.
     */
    public @NotNull Material getShowcase() {
        return this.showcase;
    }

    /**
     * Returns an immutable view of the configured quest upgrades.
     *
     * @return the configured quest upgrades.
     */
    public @NotNull List<RQuestUpgrade> getUpgrades() {
        return Collections.unmodifiableList(this.upgrades);
    }

    /**
     * Adds a quest upgrade if it has not already been configured.
     *
     * @param upgrade the upgrade entry to attach to the quest.
     */
    public void addUpgrade(final @NotNull RQuestUpgrade upgrade) {
        Objects.requireNonNull(upgrade, "upgrade cannot be null");
        if (!this.upgrades.contains(upgrade)) {
            this.upgrades.add(upgrade);
        }
    }

    /**
     * Removes the specified quest upgrade if present.
     *
     * @param upgrade the upgrade entry to remove.
     * @return {@code true} when the upgrade was removed; {@code false} otherwise.
     */
    public boolean removeUpgrade(final @NotNull RQuestUpgrade upgrade) {
        Objects.requireNonNull(upgrade, "upgrade cannot be null");
        return this.upgrades.remove(upgrade);
    }

    /**
     * Initializes the quest showcase material when the entity is constructed.
     *
     * @return the material used to represent the quest in menus.
     */
    @NotNull
    protected abstract Material initializeShowcase();

    /**
     * Compares quests using their identifier for equality.
     *
     * @param obj the object to compare against.
     * @return {@code true} when both quests share the same identifier; {@code false} otherwise.
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof final RQuest other)) return false;
        return this.identifier.equals(other.identifier);
    }

    /**
     * Generates the hash code using the quest identifier.
     *
     * @return the hash code for this quest.
     */
    @Override
    public int hashCode() {
        return this.identifier.hashCode();
    }

    /**
     * Provides a formatted representation of the quest for logging and debugging.
     *
     * @return a formatted string containing quest metadata.
     */
    @Override
    public String toString() {
        return "RQuest[id=%d, identifier=%s, levels=%d-%d]"
                .formatted(getId(), identifier, initialUpgradeLevel, maximumUpgradeLevel);
    }
}