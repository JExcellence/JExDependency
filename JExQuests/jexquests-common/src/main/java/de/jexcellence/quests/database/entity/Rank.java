package de.jexcellence.quests.database.entity;

import de.jexcellence.jehibernate.entity.base.LongIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One rank within a {@link RankTree}. Promotion is gated by the
 * JSON-encoded {@link de.jexcellence.core.api.requirement.Requirement}
 * in {@code requirementData}; rewards on promotion live in
 * {@code rewardData} as a JSON-encoded
 * {@link de.jexcellence.core.api.reward.Reward}.
 *
 * <p>Ranks carry three progression signals, inherited from RDQ's
 * design:
 * <ul>
 *   <li>{@link #orderIndex} — legacy linear ordering (still primary
 *       for sort + initial-rank discovery)</li>
 *   <li>{@link #tier} — coarse grouping (T1, T2, ...) for grid
 *       rendering; ranks in the same tier are siblings, not ordered</li>
 *   <li>{@link #previousRanks} / {@link #nextRanks} — comma-separated
 *       rank identifiers enabling true branching paths (e.g. Veteran
 *       → {Berserker, Guardian, Gladiator}). Leave blank on linear
 *       trees; the loader infers edges from {@link #orderIndex}.</li>
 * </ul>
 *
 * <p>{@link #luckPermsGroup} / {@link #prefixKey} / {@link #suffixKey}
 * are advisory metadata — nothing in the core service layer reads
 * them, but a LuckPerms integration or a prefix sync plugin can hook
 * the {@code RankPromotedEvent} and act on them.
 */
@Entity
@Table(
        name = "jexquests_rank",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_jexquests_rank_tree_identifier",
                columnNames = {"rank_tree_id", "identifier"}
        ),
        indexes = {
                @Index(name = "idx_jexquests_rank_tree", columnList = "rank_tree_id"),
                @Index(name = "idx_jexquests_rank_order", columnList = "order_index")
        }
)
public class Rank extends LongIdEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rank_tree_id", nullable = false)
    private RankTree tree;

    @Column(name = "identifier", nullable = false, length = 64)
    private String identifier;

    @Column(name = "display_name", nullable = false, length = 128)
    private String displayName;

    @Column(name = "description", length = 512)
    private String description;

    @Lob
    @Column(name = "icon_data")
    private String iconData;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Lob
    @Column(name = "requirement_data")
    private String requirementData;

    @Lob
    @Column(name = "reward_data")
    private String rewardData;

    @Column(name = "tier", nullable = false)
    private int tier = 1;

    @Column(name = "weight", nullable = false)
    private int weight = 100;

    @Column(name = "initial_rank", nullable = false)
    private boolean initialRank = false;

    @Column(name = "luck_perms_group", length = 64)
    private String luckPermsGroup;

    @Column(name = "prefix_key", length = 128)
    private String prefixKey;

    @Column(name = "suffix_key", length = 128)
    private String suffixKey;

    /** Comma-separated rank identifiers within the same tree. */
    @Column(name = "previous_ranks", length = 512)
    private String previousRanks;

    /** Comma-separated rank identifiers within the same tree. */
    @Column(name = "next_ranks", length = 512)
    private String nextRanks;

    protected Rank() {
    }

    public Rank(
            @NotNull RankTree tree,
            @NotNull String identifier,
            @NotNull String displayName,
            int orderIndex
    ) {
        this.tree = tree;
        this.identifier = identifier;
        this.displayName = displayName;
        this.orderIndex = orderIndex;
    }

    public @NotNull RankTree getTree() { return this.tree; }
    public void setTree(@NotNull RankTree tree) { this.tree = tree; }
    public @NotNull String getIdentifier() { return this.identifier; }
    public @NotNull String getDisplayName() { return this.displayName; }
    public void setDisplayName(@NotNull String displayName) { this.displayName = displayName; }
    public @Nullable String getDescription() { return this.description; }
    public void setDescription(@Nullable String description) { this.description = description; }
    public @Nullable String getIconData() { return this.iconData; }
    public void setIconData(@Nullable String iconData) { this.iconData = iconData; }
    public int getOrderIndex() { return this.orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }
    public @Nullable String getRequirementData() { return this.requirementData; }
    public void setRequirementData(@Nullable String requirementData) { this.requirementData = requirementData; }
    public @Nullable String getRewardData() { return this.rewardData; }
    public void setRewardData(@Nullable String rewardData) { this.rewardData = rewardData; }
    public int getTier() { return this.tier; }
    public void setTier(int tier) { this.tier = tier; }
    public int getWeight() { return this.weight; }
    public void setWeight(int weight) { this.weight = weight; }
    public boolean isInitialRank() { return this.initialRank; }
    public void setInitialRank(boolean initialRank) { this.initialRank = initialRank; }
    public @Nullable String getLuckPermsGroup() { return this.luckPermsGroup; }
    public void setLuckPermsGroup(@Nullable String luckPermsGroup) { this.luckPermsGroup = luckPermsGroup; }
    public @Nullable String getPrefixKey() { return this.prefixKey; }
    public void setPrefixKey(@Nullable String prefixKey) { this.prefixKey = prefixKey; }
    public @Nullable String getSuffixKey() { return this.suffixKey; }
    public void setSuffixKey(@Nullable String suffixKey) { this.suffixKey = suffixKey; }
    public @Nullable String getPreviousRanks() { return this.previousRanks; }
    public void setPreviousRanks(@Nullable String previousRanks) { this.previousRanks = previousRanks; }
    public @Nullable String getNextRanks() { return this.nextRanks; }
    public void setNextRanks(@Nullable String nextRanks) { this.nextRanks = nextRanks; }

    /** Comma-split helper for {@link #previousRanks}. */
    public @NotNull java.util.List<String> previousRankList() {
        return split(this.previousRanks);
    }

    /** Comma-split helper for {@link #nextRanks}. */
    public @NotNull java.util.List<String> nextRankList() {
        return split(this.nextRanks);
    }

    private static @NotNull java.util.List<String> split(@Nullable String raw) {
        if (raw == null || raw.isBlank()) return java.util.List.of();
        return java.util.Arrays.stream(raw.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    @Override
    public String toString() {
        // Intentionally skip this.tree.getIdentifier() — it's a lazy
        // proxy and any toString() invocation outside a Hibernate
        // session (Bukkit's error logger, IF context serialisation)
        // trips a LazyInitializationException. The rank's own
        // identifier is unique per tree in practice.
        return "Rank[" + this.identifier + "@T" + this.tier + "]";
    }
}
