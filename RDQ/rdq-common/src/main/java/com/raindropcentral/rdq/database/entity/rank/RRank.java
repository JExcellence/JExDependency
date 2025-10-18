package com.raindropcentral.rdq.database.entity.rank;


import com.raindropcentral.rdq.config.item.IconSection;
import com.raindropcentral.rdq.database.converter.IconSectionConverter;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

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

    protected RRank() {}

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

    public @NotNull String getIdentifier() {
        return this.identifier;
    }

    public @NotNull String getDisplayNameKey() {
        return this.displayNameKey;
    }

    public @NotNull String getDescriptionKey() {
        return this.descriptionKey;
    }

    public @NotNull String getAssignedLuckPermsGroup() {
        return this.assignedLuckPermsGroup;
    }

    public @NotNull String getPrefixKey() {
        return this.prefixKey;
    }

    public @NotNull String getSuffixKey() {
        return this.suffixKey;
    }

    public int getTier() {
        return this.tier;
    }

    public int getWeight() {
        return this.weight;
    }

    public boolean isInitialRank() {
        return this.isInitialRank;
    }

    public boolean isFinalRank() {
        return this.isFinalRank;
    }

    public boolean isEnabled() {
        return this.isEnabled;
    }

    public void setEnabled(final boolean enabled) {
        this.isEnabled = enabled;
    }

    public @Nullable RRankTree getRankTree() {
        return this.rankTree;
    }

    public void setRankTree(final @Nullable RRankTree rankTree) {
        this.rankTree = rankTree;
    }

    public @NotNull Set<RRankUpgradeRequirement> getUpgradeRequirements() {
        return Collections.unmodifiableSet(this.upgradeRequirements);
    }

    public @NotNull List<RRankUpgradeRequirement> getUpgradeRequirementsOrdered() {
        return this.upgradeRequirements.stream()
                .sorted(Comparator.comparingInt(RRankUpgradeRequirement::getDisplayOrder))
                .collect(Collectors.toList());
    }

    public @NotNull IconSection getIcon() {
        return this.icon;
    }

    public @NotNull List<String> getPreviousRanks() {
        return Collections.unmodifiableList(this.previousRanks);
    }

    public void setPreviousRanks(final @NotNull List<String> previousRanks) {
        this.previousRanks.clear();
        this.previousRanks.addAll(Objects.requireNonNull(previousRanks, "previousRanks cannot be null"));
    }

    public @NotNull List<String> getNextRanks() {
        return Collections.unmodifiableList(this.nextRanks);
    }

    public void setNextRanks(final @NotNull List<String> nextRanks) {
        this.nextRanks.clear();
        this.nextRanks.addAll(Objects.requireNonNull(nextRanks, "nextRanks cannot be null"));
    }

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

    public boolean removeUpgradeRequirement(final @NotNull RRankUpgradeRequirement upgradeRequirement) {
        Objects.requireNonNull(upgradeRequirement, "upgradeRequirement cannot be null");
        final boolean removed = this.upgradeRequirements.remove(upgradeRequirement);
        if (removed && upgradeRequirement.getRank() == this) {
            upgradeRequirement.setRank(null);
        }
        return removed;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof final RRank other)) return false;
        return this.identifier.equals(other.identifier);
    }

    @Override
    public int hashCode() {
        return this.identifier.hashCode();
    }

    @Override
    public String toString() {
        return "RRank[identifier=%s, tier=%d, weight=%d, enabled=%b]"
                .formatted(identifier, tier, weight, isEnabled);
    }
}