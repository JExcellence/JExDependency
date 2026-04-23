package de.jexcellence.quests.database.entity;

import de.jexcellence.jehibernate.entity.base.LongIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A perk definition. Unlock gating, activation gating, and effect
 * payload are all JSON blobs: {@code requirementData} encodes a
 * {@link de.jexcellence.core.api.requirement.Requirement} for
 * unlock; {@code rewardData} encodes a
 * {@link de.jexcellence.core.api.reward.Reward} for activation effect;
 * {@code behaviourData} is free-form config consumed by the perk's
 * registered handler.
 */
@Entity
@Table(
        name = "jexquests_perk",
        uniqueConstraints = @UniqueConstraint(columnNames = "identifier"),
        indexes = {
                @Index(name = "idx_jexquests_perk_identifier", columnList = "identifier"),
                @Index(name = "idx_jexquests_perk_category", columnList = "category"),
                @Index(name = "idx_jexquests_perk_kind", columnList = "kind")
        }
)
public class Perk extends LongIdEntity {

    @Column(name = "identifier", nullable = false, unique = true, length = 64)
    private String identifier;

    @Column(name = "category", nullable = false, length = 32)
    private String category;

    @Column(name = "display_name", nullable = false, length = 128)
    private String displayName;

    @Column(name = "description", length = 512)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 16)
    private PerkKind kind = PerkKind.PASSIVE;

    @Column(name = "cooldown_seconds", nullable = false)
    private long cooldownSeconds;

    @Lob
    @Column(name = "icon_data")
    private String iconData;

    @Lob
    @Column(name = "requirement_data")
    private String requirementData;

    @Lob
    @Column(name = "reward_data")
    private String rewardData;

    @Lob
    @Column(name = "behaviour_data")
    private String behaviourData;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    protected Perk() {
    }

    public Perk(@NotNull String identifier, @NotNull String category, @NotNull String displayName, @NotNull PerkKind kind) {
        this.identifier = identifier;
        this.category = category;
        this.displayName = displayName;
        this.kind = kind;
    }

    public @NotNull String getIdentifier() { return this.identifier; }
    public @NotNull String getCategory() { return this.category; }
    public void setCategory(@NotNull String category) { this.category = category; }
    public @NotNull String getDisplayName() { return this.displayName; }
    public void setDisplayName(@NotNull String displayName) { this.displayName = displayName; }
    public @Nullable String getDescription() { return this.description; }
    public void setDescription(@Nullable String description) { this.description = description; }
    public @NotNull PerkKind getKind() { return this.kind; }
    public void setKind(@NotNull PerkKind kind) { this.kind = kind; }
    public long getCooldownSeconds() { return this.cooldownSeconds; }
    public void setCooldownSeconds(long cooldownSeconds) { this.cooldownSeconds = cooldownSeconds; }
    public @Nullable String getIconData() { return this.iconData; }
    public void setIconData(@Nullable String iconData) { this.iconData = iconData; }
    public @Nullable String getRequirementData() { return this.requirementData; }
    public void setRequirementData(@Nullable String requirementData) { this.requirementData = requirementData; }
    public @Nullable String getRewardData() { return this.rewardData; }
    public void setRewardData(@Nullable String rewardData) { this.rewardData = rewardData; }
    public @Nullable String getBehaviourData() { return this.behaviourData; }
    public void setBehaviourData(@Nullable String behaviourData) { this.behaviourData = behaviourData; }
    public boolean isEnabled() { return this.enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    @Override
    public String toString() {
        return "Perk[" + this.identifier + "/" + this.kind + "]";
    }
}
