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

/**
 * Represents the base persistent definition of a perk in the RDQ database. Each perk encapsulates
 * its configuration metadata, enabled status, and the type-specific behavior that concrete
 * subclasses implement.
 *
 * <p>The identifier uniquely defines a perk and is used for equality checks. Keys for display name
 * and description are derived from the identifier to keep localization consistent. Player
 * relationships are exposed as an immutable view to prevent accidental modifications outside of the
 * entity lifecycle.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.2
 */
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

    /**
     * Default constructor for JPA and Hibernate. Avoid using this constructor directly; prefer the
     * parameterized constructor to ensure mandatory fields are initialized.
     */
    protected RPerk() {}

    /**
     * Creates a new perk definition with the provided identifier, configuration, and type. The
     * identifier is also used to auto-generate localization keys for the name and description.
     *
     * @param identifier the unique identifier used for persistence and localization lookup
     * @param perkSection the configuration section describing the perk behavior
     * @param perkType the category that determines how the perk is processed
     */
    public RPerk(final @NotNull String identifier, final @NotNull PerkSection perkSection, final @NotNull EPerkType perkType) {
        this.identifier = Objects.requireNonNull(identifier, "identifier cannot be null");
        this.displayNameKey = identifier + ".name";
        this.descriptionKey = identifier + ".description";
        this.perkSection = Objects.requireNonNull(perkSection, "perkSection cannot be null");
        this.perkType = Objects.requireNonNull(perkType, "perkType cannot be null");
    }

    /**
     * Gets the unique identifier of this perk.
     *
     * @return the unique identifier used for persistence and localization
     */
    public @NotNull String getIdentifier() {
        return this.identifier;
    }

    /**
     * Retrieves the localization key used to display the perk name.
     *
     * @return the MiniMessage translation key for the perk name
     */
    public @NotNull String getDisplayNameKey() {
        return this.displayNameKey;
    }

    /**
     * Updates the localization key used to resolve the perk's display name.
     *
     * @param displayNameKey the translation key to store
     */
    public void setDisplayNameKey(final @NotNull String displayNameKey) {
        this.displayNameKey = Objects.requireNonNull(displayNameKey, "displayNameKey");
    }

    /**
     * Retrieves the localization key used to display the perk description.
     *
     * @return the MiniMessage translation key for the perk description
     */
    public @NotNull String getDescriptionKey() {
        return this.descriptionKey;
    }

    /**
     * Updates the localization key used to resolve the perk's description.
     *
     * @param descriptionKey the translation key to store
     */
    public void setDescriptionKey(final @NotNull String descriptionKey) {
        this.descriptionKey = Objects.requireNonNull(descriptionKey, "descriptionKey");
    }

    /**
     * Gets the configuration section describing this perk.
     *
     * @return the perk configuration section
     */
    public @NotNull PerkSection getPerkSection() {
        return this.perkSection;
    }

    /**
     * Gets the type of perk represented by this entity.
     *
     * @return the perk type used for discriminator resolution
     */
    public @NotNull EPerkType getPerkType() {
        return this.perkType;
    }

    /**
     * Indicates whether the perk is enabled and eligible for activation.
     *
     * @return {@code true} if the perk can be activated, {@code false} otherwise
     */
    public boolean isEnabled() {
        return this.isEnabled;
    }

    /**
     * Updates whether the perk is enabled.
     *
     * @param enabled {@code true} to enable the perk, {@code false} to disable it
     */
    public void setEnabled(final boolean enabled) {
        this.isEnabled = enabled;
    }

    /**
     * Obtains the priority that determines ordering during perk processing.
     *
     * @return the priority value where higher numbers typically execute first
     */
    public int getPriority() {
        return this.priority;
    }

    /**
     * Sets the processing priority for this perk.
     *
     * @param priority the new priority value
     */
    public void setPriority(final int priority) {
        this.priority = priority;
    }

    /**
     * Gets the maximum number of players that can use this perk simultaneously.
     *
     * @return the maximum concurrent users or {@code null} when unlimited
     */
    public @Nullable Integer getMaxConcurrentUsers() {
        return this.maxConcurrentUsers;
    }

    /**
     * Sets the maximum number of concurrent users allowed for the perk.
     *
     * @param maxConcurrentUsers the new limit or {@code null} for no limit
     */
    public void setMaxConcurrentUsers(final @Nullable Integer maxConcurrentUsers) {
        this.maxConcurrentUsers = maxConcurrentUsers;
    }

    /**
     * Gets the permission required to use this perk.
     *
     * @return the permission node or {@code null} when no permission is required
     */
    public @Nullable String getRequiredPermission() {
        return this.requiredPermission;
    }

    /**
     * Sets the permission required to use this perk.
     *
     * @param requiredPermission the permission node or {@code null} for unrestricted access
     */
    public void setRequiredPermission(final @Nullable String requiredPermission) {
        this.requiredPermission = requiredPermission;
    }

    /**
     * Gets custom JSON or serialized properties attached to the perk.
     *
     * @return a serialized representation of custom properties or {@code null} when absent
     */
    public @Nullable String getCustomProperties() {
        return this.customProperties;
    }

    /**
     * Sets custom properties for the perk.
     *
     * @param customProperties a serialized representation of custom attributes, or {@code null} to
     *                         clear them
     */
    public void setCustomProperties(final @Nullable String customProperties) {
        this.customProperties = customProperties;
    }

    /**
     * Provides the collection of player perks linked to this definition.
     *
     * @return an unmodifiable view of related player perks
     */
    public @NotNull Set<RPlayerPerk> getPlayerPerks() {
        return Collections.unmodifiableSet(this.playerPerks);
    }

    /**
     * Provides the collection of unlock requirements linked to this perk.
     *
     * @return an unmodifiable view of related unlock requirements
     */
    public @NotNull Set<RPerkUnlockRequirement> getUnlockRequirements() {
        return Collections.unmodifiableSet(this.unlockRequirements);
    }

    /**
     * Provides the collection of unlock requirements linked to this perk, ordered by display order.
     *
     * @return an ordered list of unlock requirements
     */
    public @NotNull List<RPerkUnlockRequirement> getUnlockRequirementsOrdered() {
        return this.unlockRequirements.stream()
                .sorted(Comparator.comparingInt(RPerkUnlockRequirement::getDisplayOrder))
                .collect(Collectors.toList());
    }

    /**
     * Provides the collection of unlock rewards linked to this perk.
     *
     * @return an unmodifiable view of related unlock rewards
     */
    public @NotNull Set<RPerkUnlockReward> getUnlockRewards() {
        return Collections.unmodifiableSet(this.unlockRewards);
    }

    /**
     * Provides the collection of unlock rewards linked to this perk, ordered by display order.
     *
     * @return an ordered list of unlock rewards
     */
    public @NotNull List<RPerkUnlockReward> getUnlockRewardsOrdered() {
        return this.unlockRewards.stream()
                .sorted(Comparator.comparingInt(RPerkUnlockReward::getDisplayOrder))
                .collect(Collectors.toList());
    }

    /**
     * Registers the provided player perk with this perk definition while ensuring the
     * bidirectional association stays in sync.
     *
     * @param playerPerk the player perk to link with this definition
     * @return {@code true} when the player perk was newly associated, {@code false} when it was already linked
     */
    public boolean addPlayerPerk(final @NotNull RPlayerPerk playerPerk) {
        Objects.requireNonNull(playerPerk, "playerPerk cannot be null");
        final boolean added = attachPlayerPerk(playerPerk);
        if (playerPerk.getPerk() != this) {
            playerPerk.setPerk(this);
        }
        return added;
    }

    /**
     * Detaches the provided player perk from this definition while clearing the reverse
     * association when it still points at this perk.
     *
     * @param playerPerk the player perk to unlink from this definition
     * @return {@code true} when the link was removed, {@code false} when it was not present
     */
    public boolean removePlayerPerk(final @NotNull RPlayerPerk playerPerk) {
        Objects.requireNonNull(playerPerk, "playerPerk cannot be null");
        final boolean removed = detachPlayerPerk(playerPerk);
        if (removed && playerPerk.getPerk() == this) {
            playerPerk.clearPerkAssociation();
        }
        return removed;
    }

    /**
     * Registers the provided unlock requirement with this perk while ensuring the
     * bidirectional association stays in sync.
     *
     * @param unlockRequirement the unlock requirement to link with this perk
     * @return {@code true} when the unlock requirement was newly associated, {@code false} when it was already linked
     */
    public boolean addUnlockRequirement(final @NotNull RPerkUnlockRequirement unlockRequirement) {
        Objects.requireNonNull(unlockRequirement, "unlockRequirement cannot be null");
        final boolean added = attachUnlockRequirement(unlockRequirement);
        if (unlockRequirement.getPerk() != this) {
            unlockRequirement.setPerk(this);
        }
        return added;
    }

    /**
     * Detaches the provided unlock requirement from this perk while clearing the reverse
     * association when it still points at this perk.
     *
     * @param unlockRequirement the unlock requirement to unlink from this perk
     * @return {@code true} when the link was removed, {@code false} when it was not present
     */
    public boolean removeUnlockRequirement(final @NotNull RPerkUnlockRequirement unlockRequirement) {
        Objects.requireNonNull(unlockRequirement, "unlockRequirement cannot be null");
        final boolean removed = detachUnlockRequirement(unlockRequirement);
        if (removed && unlockRequirement.getPerk() == this) {
            unlockRequirement.setPerk(null);
        }
        return removed;
    }

    /**
     * Registers the provided unlock reward with this perk while ensuring the
     * bidirectional association stays in sync.
     *
     * @param unlockReward the unlock reward to link with this perk
     * @return {@code true} when the unlock reward was newly associated, {@code false} when it was already linked
     */
    public boolean addUnlockReward(final @NotNull RPerkUnlockReward unlockReward) {
        Objects.requireNonNull(unlockReward, "unlockReward cannot be null");
        final boolean added = attachUnlockReward(unlockReward);
        if (unlockReward.getPerk() != this) {
            unlockReward.setPerk(this);
        }
        return added;
    }

    /**
     * Detaches the provided unlock reward from this perk while clearing the reverse
     * association when it still points at this perk.
     *
     * @param unlockReward the unlock reward to unlink from this perk
     * @return {@code true} when the link was removed, {@code false} when it was not present
     */
    public boolean removeUnlockReward(final @NotNull RPerkUnlockReward unlockReward) {
        Objects.requireNonNull(unlockReward, "unlockReward cannot be null");
        final boolean removed = detachUnlockReward(unlockReward);
        if (removed && unlockReward.getPerk() == this) {
            unlockReward.setPerk(null);
        }
        return removed;
    }

    /**
     * Internal helper used by {@link RPlayerPerk} to ensure that bidirectional synchronization can
     * occur without triggering recursive calls when the owning side changes.
     *
     * @param playerPerk the player perk being attached
     * @return {@code true} when the player perk was added to the backing collection
     */
    final boolean attachPlayerPerk(final @NotNull RPlayerPerk playerPerk) {
        Objects.requireNonNull(playerPerk, "playerPerk cannot be null");
        return this.playerPerks.add(playerPerk);
    }

    /**
     * Internal helper used by {@link RPlayerPerk} to remove the association without clearing the
     * reverse reference when a reassignment occurs.
     *
     * @param playerPerk the player perk being detached
     * @return {@code true} when the player perk was removed from the backing collection
     */
    final boolean detachPlayerPerk(final @NotNull RPlayerPerk playerPerk) {
        Objects.requireNonNull(playerPerk, "playerPerk cannot be null");
        return this.playerPerks.remove(playerPerk);
    }

    /**
     * Internal helper used by {@link RPerkUnlockRequirement} to ensure that bidirectional synchronization can
     * occur without triggering recursive calls when the owning side changes.
     *
     * @param unlockRequirement the unlock requirement being attached
     * @return {@code true} when the unlock requirement was added to the backing collection
     */
    final boolean attachUnlockRequirement(final @NotNull RPerkUnlockRequirement unlockRequirement) {
        Objects.requireNonNull(unlockRequirement, "unlockRequirement cannot be null");
        return this.unlockRequirements.add(unlockRequirement);
    }

    /**
     * Internal helper used by {@link RPerkUnlockRequirement} to remove the association without clearing the
     * reverse reference when a reassignment occurs.
     *
     * @param unlockRequirement the unlock requirement being detached
     * @return {@code true} when the unlock requirement was removed from the backing collection
     */
    final boolean detachUnlockRequirement(final @NotNull RPerkUnlockRequirement unlockRequirement) {
        Objects.requireNonNull(unlockRequirement, "unlockRequirement cannot be null");
        return this.unlockRequirements.remove(unlockRequirement);
    }

    /**
     * Internal helper used by {@link RPerkUnlockReward} to ensure that bidirectional synchronization can
     * occur without triggering recursive calls when the owning side changes.
     *
     * @param unlockReward the unlock reward being attached
     * @return {@code true} when the unlock reward was added to the backing collection
     */
    final boolean attachUnlockReward(final @NotNull RPerkUnlockReward unlockReward) {
        Objects.requireNonNull(unlockReward, "unlockReward cannot be null");
        return this.unlockRewards.add(unlockReward);
    }

    /**
     * Internal helper used by {@link RPerkUnlockReward} to remove the association without clearing the
     * reverse reference when a reassignment occurs.
     *
     * @param unlockReward the unlock reward being detached
     * @return {@code true} when the unlock reward was removed from the backing collection
     */
    final boolean detachUnlockReward(final @NotNull RPerkUnlockReward unlockReward) {
        Objects.requireNonNull(unlockReward, "unlockReward cannot be null");
        return this.unlockRewards.remove(unlockReward);
    }

    /**
     * Executes the activation logic for the perk.
     *
     * @return {@code true} when activation succeeds, {@code false} when it fails
     */
    public abstract boolean performActivation();

    /**
     * Executes the deactivation logic for the perk.
     *
     * @return {@code true} when deactivation succeeds, {@code false} otherwise
     */
    public abstract boolean performDeactivation();

    /**
     * Determines whether the activation criteria for this perk are currently satisfied.
     *
     * @return {@code true} if activation can proceed, {@code false} otherwise
     */
    public abstract boolean canPerformActivation();

    /**
     * Performs any passive or triggered behavior associated with this perk.
     */
    public abstract void performTrigger();

    /**
     * Compares perks based on their unique identifier.
     *
     * @param obj the object to compare against
     * @return {@code true} when the other object is an {@link RPerk} with the same identifier
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof final RPerk other)) return false;
        return this.identifier.equals(other.identifier);
    }

    /**
     * Generates a hash code that aligns with {@link #equals(Object)}.
     *
     * @return the hash code derived from the identifier
     */
    @Override
    public int hashCode() {
        return this.identifier.hashCode();
    }

    /**
     * Provides a concise textual representation of the perk for logging or debugging.
     *
     * @return a formatted string containing the identifier, type, and enabled state
     */
    @Override
    public String toString() {
        return "RPerk[identifier=%s, type=%s, enabled=%b]".formatted(identifier, perkType, isEnabled);
    }
}