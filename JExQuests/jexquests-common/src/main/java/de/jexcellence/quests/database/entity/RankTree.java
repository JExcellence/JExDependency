package de.jexcellence.quests.database.entity;

import de.jexcellence.jehibernate.entity.base.LongIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A progression track — a linear or branching sequence of
 * {@link Rank}s. Players opt into a tree (via {@link QuestsPlayer#getActiveRankTree()})
 * and progress through its ranks.
 *
 * <p>Mirrors RDQ's {@code RRankTree} feature set: tree-level
 * prerequisites ({@link #prerequisiteTrees}), a minimum number of
 * other trees that must be completed ({@link #minimumTreesToBeDone}),
 * and cross-tree switching rules ({@link #allowSwitching} +
 * {@link #switchCooldownSeconds}). Prerequisites are stored as a
 * comma-separated string of tree identifiers rather than a join table
 * — avoids Hibernate fetch pitfalls when many trees reference many.
 */
@Entity
@Table(
        name = "jexquests_rank_tree",
        uniqueConstraints = @UniqueConstraint(columnNames = "identifier"),
        indexes = @Index(name = "idx_jexquests_rank_tree_identifier", columnList = "identifier")
)
public class RankTree extends LongIdEntity {

    @Column(name = "identifier", nullable = false, unique = true, length = 64)
    private String identifier;

    @Column(name = "display_name", nullable = false, length = 128)
    private String displayName;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "icon_material", length = 64)
    private String iconMaterial;

    @Column(name = "final_tree", nullable = false)
    private boolean finalTree = false;

    @Column(name = "requires_all_done", nullable = false)
    private boolean requiresAllDone = false;

    @Column(name = "minimum_trees_done", nullable = false)
    private int minimumTreesToBeDone = 0;

    /** Comma-separated list of tree identifiers that must be completed first. */
    @Column(name = "prerequisite_trees", length = 512)
    private String prerequisiteTrees;

    @Column(name = "allow_switching", nullable = false)
    private boolean allowSwitching = true;

    @Column(name = "switch_cooldown_seconds", nullable = false)
    private long switchCooldownSeconds = 0L;

    protected RankTree() {
    }

    public RankTree(@NotNull String identifier, @NotNull String displayName) {
        this.identifier = identifier;
        this.displayName = displayName;
    }

    public @NotNull String getIdentifier() { return this.identifier; }
    public @NotNull String getDisplayName() { return this.displayName; }
    public void setDisplayName(@NotNull String displayName) { this.displayName = displayName; }
    public @Nullable String getDescription() { return this.description; }
    public void setDescription(@Nullable String description) { this.description = description; }
    public int getDisplayOrder() { return this.displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public boolean isEnabled() { return this.enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public @Nullable String getIconMaterial() { return this.iconMaterial; }
    public void setIconMaterial(@Nullable String iconMaterial) { this.iconMaterial = iconMaterial; }
    public boolean isFinalTree() { return this.finalTree; }
    public void setFinalTree(boolean finalTree) { this.finalTree = finalTree; }
    public boolean isRequiresAllDone() { return this.requiresAllDone; }
    public void setRequiresAllDone(boolean requiresAllDone) { this.requiresAllDone = requiresAllDone; }
    public int getMinimumTreesToBeDone() { return this.minimumTreesToBeDone; }
    public void setMinimumTreesToBeDone(int minimumTreesToBeDone) { this.minimumTreesToBeDone = minimumTreesToBeDone; }
    public @Nullable String getPrerequisiteTrees() { return this.prerequisiteTrees; }
    public void setPrerequisiteTrees(@Nullable String prerequisiteTrees) { this.prerequisiteTrees = prerequisiteTrees; }
    public boolean isAllowSwitching() { return this.allowSwitching; }
    public void setAllowSwitching(boolean allowSwitching) { this.allowSwitching = allowSwitching; }
    public long getSwitchCooldownSeconds() { return this.switchCooldownSeconds; }
    public void setSwitchCooldownSeconds(long switchCooldownSeconds) { this.switchCooldownSeconds = switchCooldownSeconds; }

    /** Parse {@link #prerequisiteTrees} into a list of tree identifiers. */
    public @NotNull java.util.List<String> prerequisiteTreeList() {
        if (this.prerequisiteTrees == null || this.prerequisiteTrees.isBlank()) return java.util.List.of();
        return java.util.Arrays.stream(this.prerequisiteTrees.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    @Override
    public String toString() {
        return "RankTree[" + this.identifier + "]";
    }
}
