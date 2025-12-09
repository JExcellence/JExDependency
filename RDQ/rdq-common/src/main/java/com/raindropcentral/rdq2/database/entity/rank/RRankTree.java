/*
package com.raindropcentral.rdq2.database.entity.rank;

import com.raindropcentral.rdq2.config.item.IconSection;
import com.raindropcentral.rdq2.database.converter.IconSectionConverter;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.hibernate.Hibernate;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "r_rank_tree")
public final class RRankTree extends AbstractEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "identifier", unique = true, nullable = false)
    private String identifier;

    @Column(name = "display_name_key", nullable = false, unique = true)
    private String displayNameKey;

    @Column(name = "description_key", nullable = false)
    private String descriptionKey;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "minimum_rank_trees_to_be_done", nullable = false)
    private int minimumRankTreesToBeDone;

    @Column(name = "is_enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "is_final_rank_tree", nullable = false)
    private boolean finalRankTree;

    @Column(name = "icon", nullable = false, columnDefinition = "LONGTEXT")
    @Convert(converter = IconSectionConverter.class)
    private IconSection icon;

    @OneToMany(mappedBy = "rankTree", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RRank> ranks = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "r_rank_tree_prerequisites",
            joinColumns = @JoinColumn(name = "rank_tree_id"),
            inverseJoinColumns = @JoinColumn(name = "prerequisite_rank_tree_id")
    )
    private List<RRankTree> prerequisiteRankTrees = new ArrayList<>();

    @ManyToMany(mappedBy = "prerequisiteRankTrees", fetch = FetchType.LAZY)
    private List<RRankTree> unlockedRankTrees = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "r_rank_tree_connection",
            joinColumns = @JoinColumn(name = "from_rank_tree_id"),
            inverseJoinColumns = @JoinColumn(name = "to_rank_tree_id")
    )
    private List<RRankTree> connectedRankTrees = new ArrayList<>();

    @Version
    @Column(name = "version")
    private int version;

    protected RRankTree() {}

    public RRankTree(@NotNull String identifier, @NotNull String displayNameKey,
                     @NotNull String descriptionKey, @NotNull IconSection icon,
                     int displayOrder, int minimumRankTreesToBeDone) {
        this.identifier = Objects.requireNonNull(identifier);
        this.displayNameKey = Objects.requireNonNull(displayNameKey);
        this.descriptionKey = Objects.requireNonNull(descriptionKey);
        this.icon = Objects.requireNonNull(icon);
        this.displayOrder = displayOrder;
        this.minimumRankTreesToBeDone = minimumRankTreesToBeDone;
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

    public int getDisplayOrder() {
        return this.displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public int getMinimumRankTreesToBeDone() {
        return this.minimumRankTreesToBeDone;
    }

    public void setMinimumRankTreesToBeDone(int minimumRankTreesToBeDone) {
        this.minimumRankTreesToBeDone = minimumRankTreesToBeDone;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isFinalRankTree() {
        return this.finalRankTree;
    }

    public void setFinalRankTree(boolean finalRankTree) {
        this.finalRankTree = finalRankTree;
    }

    public @NotNull IconSection getIcon() {
        return this.icon;
    }

    public @NotNull List<RRank> getRanks() {
        return List.copyOf(this.ranks);
    }

    public void setRanks(@NotNull List<RRank> ranks) {
        Objects.requireNonNull(ranks);
        if (Hibernate.isInitialized(this.ranks)) {
            if (!Objects.equals(this.ranks, ranks)) {
                this.ranks.clear();
                this.ranks.addAll(ranks);
            }
        } else {
            this.ranks = new ArrayList<>(ranks);
        }
    }

    public @NotNull List<RRankTree> getPrerequisiteRankTrees() {
        return List.copyOf(this.prerequisiteRankTrees);
    }

    public void setPrerequisiteRankTrees(@NotNull List<RRankTree> prerequisiteRankTrees) {
        Objects.requireNonNull(prerequisiteRankTrees);
        if (Hibernate.isInitialized(this.prerequisiteRankTrees)) {
            if (!Objects.equals(this.prerequisiteRankTrees, prerequisiteRankTrees)) {
                this.prerequisiteRankTrees.clear();
                this.prerequisiteRankTrees.addAll(prerequisiteRankTrees);
            }
        } else {
            this.prerequisiteRankTrees = new ArrayList<>(prerequisiteRankTrees);
        }
    }

    public @NotNull List<RRankTree> getUnlockedRankTrees() {
        return List.copyOf(this.unlockedRankTrees);
    }

    public void setUnlockedRankTrees(@NotNull List<RRankTree> unlockedRankTrees) {
        Objects.requireNonNull(unlockedRankTrees);
        if (Hibernate.isInitialized(this.unlockedRankTrees)) {
            if (!Objects.equals(this.unlockedRankTrees, unlockedRankTrees)) {
                this.unlockedRankTrees.clear();
                this.unlockedRankTrees.addAll(unlockedRankTrees);
            }
        } else {
            this.unlockedRankTrees = new ArrayList<>(unlockedRankTrees);
        }
    }

    public @NotNull List<RRankTree> getConnectedRankTrees() {
        return List.copyOf(this.connectedRankTrees);
    }

    public void setConnectedRankTrees(@NotNull List<RRankTree> connectedRankTrees) {
        Objects.requireNonNull(connectedRankTrees);
        if (Hibernate.isInitialized(this.connectedRankTrees)) {
            if (!Objects.equals(this.connectedRankTrees, connectedRankTrees)) {
                this.connectedRankTrees.clear();
                this.connectedRankTrees.addAll(connectedRankTrees);
            }
        } else {
            this.connectedRankTrees = new ArrayList<>(connectedRankTrees);
        }
    }

    public int getVersion() {
        return this.version;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof RRankTree other)) return false;
        return Objects.equals(this.identifier, other.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.identifier);
    }

    @Override
    public String toString() {
        return "RRankTree[identifier=%s, displayOrder=%d, enabled=%b]"
                .formatted(identifier, displayOrder, enabled);
    }
}
*/
