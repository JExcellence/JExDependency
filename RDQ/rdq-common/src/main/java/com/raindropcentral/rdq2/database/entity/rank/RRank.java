/*
package com.raindropcentral.rdq2.database.entity.rank;

import com.raindropcentral.rdq2.config.item.IconSection;
import com.raindropcentral.rdq2.database.converter.IconSectionConverter;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

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
    private boolean initialRank;

    @Column(name = "is_final_rank")
    private boolean finalRank;

    @Column(name = "is_enabled")
    private boolean enabled = true;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rank_tree_id")
    private RRankTree rankTree;

    @OneToMany(mappedBy = "rank", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<RRankUpgradeRequirement> upgradeRequirements = new HashSet<>();

    protected RRank() {}

    public RRank(@NotNull String identifier, @NotNull String displayNameKey,
                 @NotNull String descriptionKey, @NotNull String assignedLuckPermsGroup,
                 @NotNull String prefixKey, @NotNull String suffixKey,
                 @NotNull IconSection icon, int tier, int weight) {
        this.identifier = Objects.requireNonNull(identifier);
        this.displayNameKey = Objects.requireNonNull(displayNameKey);
        this.descriptionKey = Objects.requireNonNull(descriptionKey);
        this.assignedLuckPermsGroup = Objects.requireNonNull(assignedLuckPermsGroup);
        this.prefixKey = Objects.requireNonNull(prefixKey);
        this.suffixKey = Objects.requireNonNull(suffixKey);
        this.icon = Objects.requireNonNull(icon);
        this.tier = tier;
        this.weight = weight;
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
        return this.initialRank;
    }

    public void setInitialRank(boolean initialRank) {
        this.initialRank = initialRank;
    }

    public boolean isFinalRank() {
        return this.finalRank;
    }

    public void setFinalRank(boolean finalRank) {
        this.finalRank = finalRank;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public @Nullable RRankTree getRankTree() {
        return this.rankTree;
    }

    public void setRankTree(@Nullable RRankTree rankTree) {
        this.rankTree = rankTree;
    }

    public @NotNull Set<RRankUpgradeRequirement> getUpgradeRequirements() {
        return Collections.unmodifiableSet(this.upgradeRequirements);
    }

    public @NotNull List<RRankUpgradeRequirement> getUpgradeRequirementsOrdered() {
        return this.upgradeRequirements.stream()
                .sorted(Comparator.comparingInt(RRankUpgradeRequirement::getDisplayOrder))
                .toList();
    }

    public @NotNull IconSection getIcon() {
        return this.icon;
    }

    public @NotNull List<String> getPreviousRanks() {
        return List.copyOf(this.previousRanks);
    }

    public void setPreviousRanks(@NotNull List<String> previousRanks) {
        this.previousRanks.clear();
        this.previousRanks.addAll(Objects.requireNonNull(previousRanks));
    }

    public @NotNull List<String> getNextRanks() {
        return List.copyOf(this.nextRanks);
    }

    public void setNextRanks(@NotNull List<String> nextRanks) {
        this.nextRanks.clear();
        this.nextRanks.addAll(Objects.requireNonNull(nextRanks));
    }

    public boolean addUpgradeRequirement(@NotNull RRankUpgradeRequirement upgradeRequirement) {
        Objects.requireNonNull(upgradeRequirement);
        if (this.upgradeRequirements.contains(upgradeRequirement)) {
            return false;
        }
        boolean added = this.upgradeRequirements.add(upgradeRequirement);
        if (added && upgradeRequirement.getRank() != this) {
            upgradeRequirement.setRank(this);
        }
        return added;
    }

    public boolean removeUpgradeRequirement(@NotNull RRankUpgradeRequirement upgradeRequirement) {
        Objects.requireNonNull(upgradeRequirement);
        boolean removed = this.upgradeRequirements.remove(upgradeRequirement);
        if (removed && upgradeRequirement.getRank() == this) {
            upgradeRequirement.setRank(null);
        }
        return removed;
    }

    public void replaceUpgradeRequirements(@NotNull List<RRankUpgradeRequirement> requirements) {
        Objects.requireNonNull(requirements);
        this.upgradeRequirements.clear();
        for (RRankUpgradeRequirement req : requirements) {
            addUpgradeRequirement(req);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof RRank other)) return false;
        return this.identifier.equals(other.identifier);
    }

    @Override
    public int hashCode() {
        return this.identifier.hashCode();
    }

    @Override
    public String toString() {
        return "RRank[identifier=%s, tier=%d, weight=%d, enabled=%b]"
                .formatted(identifier, tier, weight, enabled);
    }
}*/
