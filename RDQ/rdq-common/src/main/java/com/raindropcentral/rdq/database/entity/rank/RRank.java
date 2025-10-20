package com.raindropcentral.rdq.database.entity.rank;


import com.raindropcentral.rdq.config.item.IconSection;
import com.raindropcentral.rdq.database.converter.IconSectionConverter;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a rank entity that is persisted to the database and defines how a player rank behaves
 * and connects to adjacent ranks inside a rank tree.
 *
 * <p>The entity stores identifying metadata, localization keys, icon information, ordering weights,
 * and upgrade requirements. Utility methods are provided for managing relationships to previous and
 * next ranks as well as the upgrade requirements associated with the rank.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@Entity
@Table(name = "r_rank")
public class RRank extends AbstractEntity {

    @Column(name = "identifier", nullable = false, unique = true)
    private String identifier;

    @Column(name = "display_name_key", nullable = false, unique = true)
    private String displayNameKey;

    @Column(name = "description_key", nullable = false)
    private String descriptionKey;

    @Column(name = "assigned_luckperms_group", nullable = false)
    private String assignedLuckPermsGroup;

    @Column(name = "prefix_key", nullable = false)
    private String prefixKey;

    @Column(name = "suffix_key", nullable = false)
    private String suffixKey;

    @Column(name = "tier", nullable = false)
    private int tier;

    @Column(name = "weight", nullable = false)
    private int weight;

    @Column(name = "is_initial_rank")
    private boolean isInitialRank;

    @Column(name = "is_final_rank")
    private boolean isFinalRank;

    @Column(name = "is_enabled")
    private boolean isEnabled = true;

    @Column(name = "icon", nullable = false, columnDefinition = "LONGTEXT")
    @Convert(converter = IconSectionConverter.class)
    private IconSection icon;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "r_rank_previous_ranks", joinColumns = @JoinColumn(name = "rank_id"))
    @Column(name = "previous_rank_identifier")
    private List<String> previousRanks = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "r_rank_next_ranks", joinColumns = @JoinColumn(name = "rank_id"))
    @Column(name = "next_rank_identifier")
    private List<String> nextRanks = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "rank_tree_id", nullable = true)
    private RRankTree rankTree;

    @OneToMany(mappedBy = "rank", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<RRankUpgradeRequirement> upgradeRequirements = new HashSet<>();

    /**
     * Protected no-args constructor required by Hibernate.
     */
    protected RRank() {}

    /**
     * Creates a new rank instance with the supplied metadata and icon information.
     *
     * @param identifier             the unique identifier for the rank
     * @param displayNameKey         the localization key for the rank name
     * @param descriptionKey         the localization key for the rank description
     * @param assignedLuckPermsGroup the LuckPerms group assigned when the rank is active
     * @param prefixKey              the localization key for the rank prefix
     * @param suffixKey              the localization key for the rank suffix
     * @param icon                   the icon section that represents the rank visually
     * @param isInitialRank          whether the rank is the starting point in its tree
     * @param tier                   the tier that defines hierarchy within the tree
     * @param weight                 the weight used for ordering comparisons
     * @param rankTree               the tree to which this rank belongs, if any
     */
    public RRank(final @NotNull String identifier, final @NotNull String displayNameKey,
                 final @NotNull String descriptionKey, final @NotNull String assignedLuckPermsGroup,
                 final @NotNull String prefixKey, final @NotNull String suffixKey,
                 final @NotNull IconSection icon, final boolean isInitialRank,
                 final int tier, final int weight, final @Nullable RRankTree rankTree) {
        this.identifier = Objects.requireNonNull(identifier, "identifier cannot be null");
        this.displayNameKey = Objects.requireNonNull(displayNameKey, "displayNameKey cannot be null");
        this.descriptionKey = Objects.requireNonNull(descriptionKey, "descriptionKey cannot be null");
        this.assignedLuckPermsGroup = Objects.requireNonNull(assignedLuckPermsGroup, "assignedLuckPermsGroup cannot be null");
        this.prefixKey = Objects.requireNonNull(prefixKey, "prefixKey cannot be null");
        this.suffixKey = Objects.requireNonNull(suffixKey, "suffixKey cannot be null");
        this.icon = Objects.requireNonNull(icon, "icon cannot be null");
        this.isInitialRank = isInitialRank;
        this.tier = tier;
        this.weight = weight;
        this.rankTree = rankTree;
    }

    /**
     * Retrieves the unique identifier assigned to the rank.
     *
     * @return the unique rank identifier
     */
    public @NotNull String getIdentifier() {
        return this.identifier;
    }

    /**
     * Retrieves the localization key used for the rank's display name.
     *
     * @return the localization key for the display name
     */
    public @NotNull String getDisplayNameKey() {
        return this.displayNameKey;
    }

    /**
     * Retrieves the localization key used for describing the rank.
     *
     * @return the localization key for the description
     */
    public @NotNull String getDescriptionKey() {
        return this.descriptionKey;
    }

    /**
     * Provides the LuckPerms group that is granted when the rank is assigned.
     *
     * @return the LuckPerms group name
     */
    public @NotNull String getAssignedLuckPermsGroup() {
        return this.assignedLuckPermsGroup;
    }

    /**
     * Retrieves the localization key representing the rank prefix.
     *
     * @return the localization key for the prefix
     */
    public @NotNull String getPrefixKey() {
        return this.prefixKey;
    }

    /**
     * Retrieves the localization key representing the rank suffix.
     *
     * @return the localization key for the suffix
     */
    public @NotNull String getSuffixKey() {
        return this.suffixKey;
    }

    /**
     * Obtains the tier indicating the rank's hierarchical level.
     *
     * @return the tier value
     */
    public int getTier() {
        return this.tier;
    }

    /**
     * Obtains the weight used when ordering ranks.
     *
     * @return the ordering weight
     */
    public int getWeight() {
        return this.weight;
    }

    /**
     * Indicates whether the rank is the initial rank of the tree.
     *
     * @return {@code true} if this rank is the initial rank, otherwise {@code false}
     */
    public boolean isInitialRank() {
        return this.isInitialRank;
    }

    /**
     * Indicates whether the rank is the final rank of the tree.
     *
     * @return {@code true} if this rank is the final rank, otherwise {@code false}
     */
    public boolean isFinalRank() {
        return this.isFinalRank;
    }

    /**
     * Determines if the rank is currently enabled.
     *
     * @return {@code true} when the rank is enabled
     */
    public boolean isEnabled() {
        return this.isEnabled;
    }

    /**
     * Updates whether the rank is enabled.
     *
     * @param enabled {@code true} to enable the rank, {@code false} to disable it
     */
    public void setEnabled(final boolean enabled) {
        this.isEnabled = enabled;
    }

    /**
     * Provides the rank tree associated with this rank, if one exists.
     *
     * @return the linked rank tree or {@code null} when unassigned
     */
    public @Nullable RRankTree getRankTree() {
        return this.rankTree;
    }

    /**
     * Assigns the rank tree that this rank belongs to.
     *
     * @param rankTree the tree to associate with this rank, or {@code null} to detach it
     */
    public void setRankTree(final @Nullable RRankTree rankTree) {
        this.rankTree = rankTree;
    }

    /**
     * Retrieves an unmodifiable view of the upgrade requirements linked to the rank.
     *
     * @return an immutable set of upgrade requirements
     */
    public @NotNull Set<RRankUpgradeRequirement> getUpgradeRequirements() {
        return Collections.unmodifiableSet(this.upgradeRequirements);
    }

    /**
     * Retrieves the upgrade requirements ordered by their display order.
     *
     * @return a list of requirements sorted by display order
     */
    public @NotNull List<RRankUpgradeRequirement> getUpgradeRequirementsOrdered() {
        return this.upgradeRequirements.stream()
                .sorted(Comparator.comparingInt(RRankUpgradeRequirement::getDisplayOrder))
                .collect(Collectors.toList());
    }

    /**
     * Provides the icon section used for representing the rank.
     *
     * @return the icon section configuration
     */
    public @NotNull IconSection getIcon() {
        return this.icon;
    }

    /**
     * Lists the identifiers of the ranks that precede this rank.
     *
     * @return an immutable list containing the identifiers of previous ranks
     */
    public @NotNull List<String> getPreviousRanks() {
        return Collections.unmodifiableList(this.previousRanks);
    }

    /**
     * Replaces the collection of identifiers for previous ranks that lead to this rank.
     *
     * @param previousRanks the ordered identifiers for previous ranks
     */
    public void setPreviousRanks(final @NotNull List<String> previousRanks) {
        this.previousRanks.clear();
        this.previousRanks.addAll(Objects.requireNonNull(previousRanks, "previousRanks cannot be null"));
    }

    /**
     * Lists the identifiers of the ranks that follow this rank.
     *
     * @return an immutable list containing the identifiers of next ranks
     */
    public @NotNull List<String> getNextRanks() {
        return Collections.unmodifiableList(this.nextRanks);
    }

    /**
     * Replaces the collection of identifiers for ranks that can be unlocked after this rank.
     *
     * @param nextRanks the ordered identifiers for next ranks
     */
    public void setNextRanks(final @NotNull List<String> nextRanks) {
        this.nextRanks.clear();
        this.nextRanks.addAll(Objects.requireNonNull(nextRanks, "nextRanks cannot be null"));
    }

    /**
     * Adds an upgrade requirement to the rank if it is not already present and updates the reverse
     * association when necessary.
     *
     * @param upgradeRequirement the requirement to add
     * @return {@code true} when the requirement was added, {@code false} if it was already present
     */
    public boolean addUpgradeRequirement(final @NotNull RRankUpgradeRequirement upgradeRequirement) {
        Objects.requireNonNull(upgradeRequirement, "upgradeRequirement cannot be null");
        if (this.upgradeRequirements.contains(upgradeRequirement)) {
            return false;
        }
        final boolean added = this.upgradeRequirements.add(upgradeRequirement);
        if (added && upgradeRequirement.getRank() != this) {
            upgradeRequirement.setRank(this);
        }
        return added;
    }

    /**
     * Removes an upgrade requirement from the rank and clears the reverse association when the
     * removal succeeds.
     *
     * @param upgradeRequirement the requirement to remove
     * @return {@code true} when the requirement was removed, {@code false} otherwise
     */
    public boolean removeUpgradeRequirement(final @NotNull RRankUpgradeRequirement upgradeRequirement) {
        Objects.requireNonNull(upgradeRequirement, "upgradeRequirement cannot be null");
        final boolean removed = this.upgradeRequirements.remove(upgradeRequirement);
        if (removed && upgradeRequirement.getRank() == this) {
            upgradeRequirement.setRank(null);
        }
        return removed;
    }

    /**
     * Compares this rank to another object based on the rank identifier.
     *
     * @param obj the object to compare with this rank
     * @return {@code true} when the identifiers match, otherwise {@code false}
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof final RRank other)) return false;
        return this.identifier.equals(other.identifier);
    }

    /**
     * Computes the hash code for the rank using its identifier.
     *
     * @return the hash code derived from the identifier
     */
    @Override
    public int hashCode() {
        return this.identifier.hashCode();
    }

    /**
     * Provides a human-readable representation of the rank.
     *
     * @return a string summarizing the rank state
     */
    @Override
    public String toString() {
        return "RRank[identifier=%s, tier=%d, weight=%d, enabled=%b]"
                .formatted(identifier, tier, weight, isEnabled);
    }
}
