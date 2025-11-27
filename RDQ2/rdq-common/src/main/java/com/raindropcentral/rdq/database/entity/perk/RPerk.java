package com.raindropcentral.rdq.database.entity.perk;

import com.raindropcentral.rdq.config.perk.PerkSection;
import com.raindropcentral.rdq.database.converter.PerkSectionConverter;
import com.raindropcentral.rdq.type.EPerkType;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.*;
import java.util.stream.Collectors;


@Entity
@Table(name = "r_perk")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "perk_type_discriminator", discriminatorType = DiscriminatorType.STRING)
public abstract class RPerk extends AbstractEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "identifier", unique = true, nullable = false)
    private String identifier;

    @Column(name = "display_name_key", nullable = false)
    private String displayNameKey;

    @Column(name = "description_key", nullable = false)
    private String descriptionKey;

    @Column(name = "perk_config", nullable = false, columnDefinition = "LONGTEXT")
    @Convert(converter = PerkSectionConverter.class)
    private PerkSection perkSection;

    @Column(name = "perk_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private EPerkType perkType;

    @Column(name = "is_enabled")
    private boolean isEnabled = true;

    @Column(name = "priority")
    private int priority = 0;

    @Column(name = "max_concurrent_users")
    private Integer maxConcurrentUsers;

    @Column(name = "required_permission")
    private String requiredPermission;

    @Column(name = "custom_properties", columnDefinition = "LONGTEXT")
    private String customProperties;

    @OneToMany(mappedBy = "perk", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<RPlayerPerk> playerPerks = new HashSet<>();

    @OneToMany(mappedBy = "perk", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<RPerkUnlockRequirement> unlockRequirements = new HashSet<>();

    @OneToMany(mappedBy = "perk", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<RPerkUnlockReward> unlockRewards = new HashSet<>();


    protected RPerk() {}

    public RPerk(@NotNull String identifier, @NotNull PerkSection perkSection, @NotNull EPerkType perkType) {
        this.identifier = Objects.requireNonNull(identifier);
        this.displayNameKey = identifier + ".name";
        this.descriptionKey = identifier + ".description";
        this.perkSection = Objects.requireNonNull(perkSection);
        this.perkType = Objects.requireNonNull(perkType);
    }

    public @NotNull String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(@NotNull String identifier) {
        this.identifier = Objects.requireNonNull(identifier);
    }

    public @NotNull String getDisplayNameKey() {
        return displayNameKey;
    }

    public void setDisplayNameKey(@NotNull String displayNameKey) {
        this.displayNameKey = Objects.requireNonNull(displayNameKey);
    }

    public @NotNull String getDescriptionKey() {
        return descriptionKey;
    }

    public void setDescriptionKey(@NotNull String descriptionKey) {
        this.descriptionKey = Objects.requireNonNull(descriptionKey);
    }

    public @NotNull PerkSection getPerkSection() {
        return perkSection;
    }

    public void setPerkSection(@NotNull PerkSection perkSection) {
        this.perkSection = Objects.requireNonNull(perkSection);
    }

    public @NotNull EPerkType getPerkType() {
        return perkType;
    }

    public void setPerkType(@NotNull EPerkType perkType) {
        this.perkType = Objects.requireNonNull(perkType);
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public @Nullable Integer getMaxConcurrentUsers() {
        return maxConcurrentUsers;
    }

    public void setMaxConcurrentUsers(@Nullable Integer maxConcurrentUsers) {
        this.maxConcurrentUsers = maxConcurrentUsers;
    }

    public @Nullable String getRequiredPermission() {
        return requiredPermission;
    }

    public void setRequiredPermission(@Nullable String requiredPermission) {
        this.requiredPermission = requiredPermission;
    }

    public @Nullable String getCustomProperties() {
        return customProperties;
    }

    public void setCustomProperties(@Nullable String customProperties) {
        this.customProperties = customProperties;
    }

    public @NotNull Set<RPlayerPerk> getPlayerPerks() {
        return Collections.unmodifiableSet(playerPerks);
    }

    public @NotNull Set<RPerkUnlockRequirement> getUnlockRequirements() {
        return Collections.unmodifiableSet(unlockRequirements);
    }

    public @NotNull List<RPerkUnlockRequirement> getUnlockRequirementsOrdered() {
        return unlockRequirements.stream()
                .sorted(Comparator.comparingInt(RPerkUnlockRequirement::getDisplayOrder))
                .toList();
    }

    public @NotNull Set<RPerkUnlockReward> getUnlockRewards() {
        return Collections.unmodifiableSet(unlockRewards);
    }

    public @NotNull List<RPerkUnlockReward> getUnlockRewardsOrdered() {
        return unlockRewards.stream()
                .sorted(Comparator.comparingInt(RPerkUnlockReward::getDisplayOrder))
                .toList();
    }

    public boolean addPlayerPerk(@NotNull RPlayerPerk playerPerk) {
        Objects.requireNonNull(playerPerk);
        var added = attachPlayerPerk(playerPerk);
        if (playerPerk.getPerk() != this) {
            playerPerk.setPerk(this);
        }
        return added;
    }

    public boolean removePlayerPerk(@NotNull RPlayerPerk playerPerk) {
        Objects.requireNonNull(playerPerk);
        var removed = detachPlayerPerk(playerPerk);
        if (removed && playerPerk.getPerk() == this) {
            playerPerk.clearPerkAssociation();
        }
        return removed;
    }

    public boolean addUnlockRequirement(@NotNull RPerkUnlockRequirement unlockRequirement) {
        Objects.requireNonNull(unlockRequirement);
        var added = attachUnlockRequirement(unlockRequirement);
        if (unlockRequirement.getPerk() != this) {
            unlockRequirement.setPerk(this);
        }
        return added;
    }

    public boolean removeUnlockRequirement(@NotNull RPerkUnlockRequirement unlockRequirement) {
        Objects.requireNonNull(unlockRequirement);
        var removed = detachUnlockRequirement(unlockRequirement);
        if (removed && unlockRequirement.getPerk() == this) {
            unlockRequirement.setPerk(null);
        }
        return removed;
    }

    public boolean addUnlockReward(@NotNull RPerkUnlockReward unlockReward) {
        Objects.requireNonNull(unlockReward);
        var added = attachUnlockReward(unlockReward);
        if (unlockReward.getPerk() != this) {
            unlockReward.setPerk(this);
        }
        return added;
    }

    public boolean removeUnlockReward(@NotNull RPerkUnlockReward unlockReward) {
        Objects.requireNonNull(unlockReward);
        var removed = detachUnlockReward(unlockReward);
        if (removed && unlockReward.getPerk() == this) {
            unlockReward.setPerk(null);
        }
        return removed;
    }

    final boolean attachPlayerPerk(@NotNull RPlayerPerk playerPerk) {
        Objects.requireNonNull(playerPerk);
        return playerPerks.add(playerPerk);
    }

    final boolean detachPlayerPerk(@NotNull RPlayerPerk playerPerk) {
        Objects.requireNonNull(playerPerk);
        return playerPerks.remove(playerPerk);
    }

    public void replaceUnlockRequirements(@NotNull Collection<RPerkUnlockRequirement> newRequirements) {
        Objects.requireNonNull(newRequirements);
        unlockRequirements.clear();
        newRequirements.forEach(this::addUnlockRequirement);
    }

    final boolean attachUnlockRequirement(@NotNull RPerkUnlockRequirement unlockRequirement) {
        Objects.requireNonNull(unlockRequirement);
        return unlockRequirements.add(unlockRequirement);
    }

    final boolean detachUnlockRequirement(@NotNull RPerkUnlockRequirement unlockRequirement) {
        Objects.requireNonNull(unlockRequirement);
        return unlockRequirements.remove(unlockRequirement);
    }

    final boolean attachUnlockReward(@NotNull RPerkUnlockReward unlockReward) {
        Objects.requireNonNull(unlockReward);
        return unlockRewards.add(unlockReward);
    }

    final boolean detachUnlockReward(@NotNull RPerkUnlockReward unlockReward) {
        Objects.requireNonNull(unlockReward);
        return unlockRewards.remove(unlockReward);
    }

    public abstract boolean performActivation();

    public abstract boolean performDeactivation();

    public abstract boolean canPerformActivation();

    public abstract void performTrigger();

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof RPerk other)) return false;
        return identifier.equals(other.identifier);
    }

    @Override
    public int hashCode() {
        return identifier.hashCode();
    }

    @Override
    public String toString() {
        return "RPerk[identifier=%s, type=%s, enabled=%b]".formatted(identifier, perkType, isEnabled);
    }
}