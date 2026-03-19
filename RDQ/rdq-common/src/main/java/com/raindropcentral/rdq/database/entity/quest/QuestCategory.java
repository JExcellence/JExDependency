package com.raindropcentral.rdq.database.entity.quest;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Database entity representing a quest category.
 * <p>
 * Categories organize quests into logical groups (e.g., "Mining", "Combat", "Daily").
 * Each category can have its own requirements and rewards that apply to all quests within it.
 * </p>
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
@Entity
@Table(name = "rdq_quest_categories", indexes = {
    @Index(name = "idx_category_identifier", columnList = "identifier", unique = true)
})
public class QuestCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "identifier", nullable = false, unique = true, length = 100)
    private String identifier;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "icon_material", length = 50)
    private String iconMaterial;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<Quest> quests = new ArrayList<>();

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuestCategoryRequirement> requirements = new ArrayList<>();

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuestCategoryReward> rewards = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Default constructor for JPA.
     */
    public QuestCategory() {
    }

    /**
     * Creates a new quest category with the specified identifier.
     *
     * @param identifier the unique category identifier
     */
    public QuestCategory(@NotNull final String identifier) {
        this.identifier = Objects.requireNonNull(identifier, "identifier cannot be null");
        this.enabled = true;
        this.sortOrder = 0;
    }

    /**
     * Gets the database ID.
     *
     * @return the ID
     */
    @Nullable
    public Long getId() {
        return id;
    }

    /**
     * Gets the unique category identifier.
     *
     * @return the identifier
     */
    @NotNull
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Sets the unique category identifier.
     *
     * @param identifier the identifier
     */
    public void setIdentifier(@NotNull final String identifier) {
        this.identifier = Objects.requireNonNull(identifier, "identifier cannot be null");
    }

    /**
     * Gets the display name.
     *
     * @return the display name
     */
    @NotNull
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Sets the display name.
     *
     * @param displayName the display name
     */
    public void setDisplayName(@NotNull final String displayName) {
        this.displayName = Objects.requireNonNull(displayName, "displayName cannot be null");
    }

    /**
     * Gets the description.
     *
     * @return the description
     */
    @Nullable
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description.
     *
     * @param description the description
     */
    public void setDescription(@Nullable final String description) {
        this.description = description;
    }

    /**
     * Gets the icon material name.
     *
     * @return the icon material
     */
    @Nullable
    public String getIconMaterial() {
        return iconMaterial;
    }

    /**
     * Sets the icon material name.
     *
     * @param iconMaterial the icon material
     */
    public void setIconMaterial(@Nullable final String iconMaterial) {
        this.iconMaterial = iconMaterial;
    }

    /**
     * Checks if the category is enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets whether the category is enabled.
     *
     * @param enabled true if enabled
     */
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Gets the sort order.
     *
     * @return the sort order
     */
    public int getSortOrder() {
        return sortOrder;
    }

    /**
     * Sets the sort order.
     *
     * @param sortOrder the sort order
     */
    public void setSortOrder(final int sortOrder) {
        this.sortOrder = sortOrder;
    }

    /**
     * Gets the display order for progression system compatibility.
     * <p>
     * This is an alias for {@link #getSortOrder()} to maintain compatibility
     * with progression system interfaces.
     * </p>
     *
     * @return the display order (same as sort order)
     */
    public int getDisplayOrder() {
        return sortOrder;
    }

    /**
     * Gets the quests in this category.
     *
     * @return the quests
     */
    @NotNull
    public List<Quest> getQuests() {
        return quests;
    }

    /**
     * Sets the quests in this category.
     *
     * @param quests the quests
     */
    public void setQuests(@NotNull final List<Quest> quests) {
        this.quests = Objects.requireNonNull(quests, "quests cannot be null");
    }

    /**
     * Gets the category requirements.
     *
     * @return the requirements
     */
    @NotNull
    public List<QuestCategoryRequirement> getRequirements() {
        return requirements;
    }

    /**
     * Sets the category requirements.
     *
     * @param requirements the requirements
     */
    public void setRequirements(@NotNull final List<QuestCategoryRequirement> requirements) {
        this.requirements = Objects.requireNonNull(requirements, "requirements cannot be null");
    }

    /**
     * Gets the category rewards.
     *
     * @return the rewards
     */
    @NotNull
    public List<QuestCategoryReward> getRewards() {
        return rewards;
    }

    /**
     * Sets the category rewards.
     *
     * @param rewards the rewards
     */
    public void setRewards(@NotNull final List<QuestCategoryReward> rewards) {
        this.rewards = Objects.requireNonNull(rewards, "rewards cannot be null");
    }

    /**
     * Gets the creation timestamp.
     *
     * @return the creation timestamp
     */
    @NotNull
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Gets the last update timestamp.
     *
     * @return the update timestamp
     */
    @NotNull
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Adds a requirement to this category.
     *
     * @param requirement the requirement to add
     */
    public void addRequirement(@NotNull final QuestCategoryRequirement requirement) {
        Objects.requireNonNull(requirement, "requirement cannot be null");
        if (!requirements.contains(requirement)) {
            requirements.add(requirement);
        }
    }

    /**
     * Adds a reward to this category.
     *
     * @param reward the reward to add
     */
    public void addReward(@NotNull final QuestCategoryReward reward) {
        Objects.requireNonNull(reward, "reward cannot be null");
        if (!rewards.contains(reward)) {
            rewards.add(reward);
        }
    }

    /**
     * Adds a quest to this category.
     *
     * @param quest the quest to add
     */
    public void addQuest(@NotNull final Quest quest) {
        Objects.requireNonNull(quest, "quest cannot be null");
        if (!quests.contains(quest)) {
            quests.add(quest);
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof QuestCategory)) return false;
        final QuestCategory that = (QuestCategory) o;
        return Objects.equals(identifier, that.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier);
    }

    @Override
    public String toString() {
        return "QuestCategory{" +
                "id=" + id +
                ", identifier='" + identifier + '\'' +
                ", displayName='" + displayName + '\'' +
                ", enabled=" + enabled +
                ", sortOrder=" + sortOrder +
                '}';
    }
}
