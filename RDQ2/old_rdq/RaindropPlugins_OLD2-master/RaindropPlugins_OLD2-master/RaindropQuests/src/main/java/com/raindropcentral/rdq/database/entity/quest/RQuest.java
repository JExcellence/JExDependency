package com.raindropcentral.rdq.database.entity.quest;

import com.raindropcentral.rplatform.database.converter.BasicMaterialConverter;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base entity representing a quest in the RaindropQuests system.
 * <p>
 * This class defines the core structure and persistence mapping for all quests, including
 * identifier, upgrade levels, showcase information, and associated quest upgrades.
 * Subclasses must implement the logic for determining the showcase material.
 * </p>
 *
 * <p>
 * The class is mapped as a JPA entity and uses the {@link BasicMaterialConverter} for
 * persisting the showcase material. Quest upgrades are managed as a one-to-many relationship.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
@Entity
public abstract class RQuest extends AbstractEntity {

    /**
     * Unique string identifier for the quest.
     */
    @Column(name = "identifier", unique = true, nullable = false)
    private String identifier;

    /**
     * The initial upgrade level for this quest.
     */
    @Column(name = "initial_upgrade_level", nullable = false)
    private int initialUpgradeLevel;

    /**
     * The maximum upgrade level for this quest.
     */
    @Column(name = "maximum_upgrade_level", nullable = false)
    private int maximumUpgradeLevel;

    /**
     * The material used to visually represent this quest in the UI.
     * <p>
     * Stored as a BLOB using {@link BasicMaterialConverter}.
     * </p>
     */
    @Column(name = "showcase_item", nullable = false, columnDefinition = "BLOB")
    @Convert(converter = BasicMaterialConverter.class)
    private Material showcase;

    /**
     * Internationalization key for the quest's display name.
     */
    @Column(name = "showcase_i18n_name_key", nullable = false)
    private String showcaseI18nNameKey;

    /**
     * Internationalization key for the quest's lore/description.
     */
    @Column(name = "showcase_i18n_lore_key", nullable = false)
    private String showcaseI18nLoreKey;

    /**
     * List of upgrades available for this quest.
     * <p>
     * Managed as a one-to-many relationship with {@link RQuestUpgrade}.
     * </p>
     */
    @OneToMany(mappedBy = "quest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RQuestUpgrade> upgrades = new ArrayList<>();

    /**
     * Protected no-argument constructor for JPA.
     */
    protected RQuest() {
    }

    /**
     * Constructs a new {@code RQuest} with the specified identifier and upgrade levels.
     * <p>
     * Also initializes the showcase material and sets default i18n keys for name and lore.
     * </p>
     *
     * @param identifier          the unique identifier for the quest
     * @param initialUpgradeLevel the initial upgrade level for the quest
     * @param maximumUpgradeLevel the maximum upgrade level for the quest
     */
    public RQuest(
            final @NotNull String identifier,
            final int initialUpgradeLevel,
            final int maximumUpgradeLevel
    ) {
        this.identifier = identifier;
        this.initialUpgradeLevel = initialUpgradeLevel;
        this.maximumUpgradeLevel = maximumUpgradeLevel;
        this.showcaseI18nNameKey = "quest.name";
        this.showcaseI18nLoreKey = "quest.lore";
        this.showcase = this.initializeShowcase();
    }

    /**
     * Gets the unique identifier for this quest.
     *
     * @return the quest identifier
     */
    public String getIdentifier() {
        return this.identifier;
    }

    /**
     * Gets the initial upgrade level for this quest.
     *
     * @return the initial upgrade level
     */
    public int getInitialUpgradeLevel() {
        return this.initialUpgradeLevel;
    }

    /**
     * Gets the maximum upgrade level for this quest.
     *
     * @return the maximum upgrade level
     */
    public int getMaximumUpgradeLevel() {
        return this.maximumUpgradeLevel;
    }

    /**
     * Gets the material used to showcase this quest.
     *
     * @return the showcase material
     */
    public Material getShowcase() {
        return this.showcase;
    }

    /**
     * Gets the internationalization key for the quest's display name.
     *
     * @return the i18n name key
     */
    public String getShowcaseI18nNameKey() {
        return this.showcaseI18nNameKey;
    }

    /**
     * Gets the internationalization key for the quest's lore/description.
     *
     * @return the i18n lore key
     */
    public String getShowcaseI18nLoreKey() {
        return this.showcaseI18nLoreKey;
    }

    /**
     * Gets the list of upgrades available for this quest.
     *
     * @return the list of {@link RQuestUpgrade} entities
     */
    public List<RQuestUpgrade> getUpgrades() {
        return this.upgrades;
    }

    /**
     * Initializes and returns the showcase material for this quest.
     * <p>
     * Subclasses must implement this to specify their visual representation.
     * </p>
     *
     * @return the showcase {@link Material}
     */
    @NotNull
    protected abstract Material initializeShowcase();
}
