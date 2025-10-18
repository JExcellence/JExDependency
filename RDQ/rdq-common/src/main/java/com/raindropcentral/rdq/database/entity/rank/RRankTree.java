package com.raindropcentral.rdq.database.entity.rank;

import com.raindropcentral.rdq.config.item.IconSection;
import com.raindropcentral.rdq.database.converter.IconSectionConverter;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.hibernate.Hibernate;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
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
    private boolean isEnabled = true;

    @Column(name = "is_final_rank_tree", nullable = false)
    private boolean isFinalRankTree;

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

    public RRankTree(final @NotNull String identifier, final @NotNull String displayNameKey,
                     final @NotNull String descriptionKey, final @NotNull IconSection icon,
                     final int displayOrder, final int minimumRankTreesToBeDone,
                     final boolean isEnabled, final boolean isFinalRankTree) {
        this.identifier = Objects.requireNonNull(identifier, "identifier cannot be null");
        this.displayNameKey = Objects.requireNonNull(displayNameKey, "displayNameKey cannot be null");
        this.descriptionKey = Objects.requireNonNull(descriptionKey, "descriptionKey cannot be null");
        this.icon = Objects.requireNonNull(icon, "icon cannot be null");
        this.displayOrder = displayOrder;
        this.minimumRankTreesToBeDone = minimumRankTreesToBeDone;
        this.isEnabled = isEnabled;
        this.isFinalRankTree = isFinalRankTree;
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

    public void setDisplayOrder(final int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public int getMinimumRankTreesToBeDone() {
        return this.minimumRankTreesToBeDone;
    }

    public void setMinimumRankTreesToBeDone(final int minimumRankTreesToBeDone) {
        this.minimumRankTreesToBeDone = minimumRankTreesToBeDone;
    }

    public boolean isEnabled() {
        return this.isEnabled;
    }

    public void setEnabled(final boolean enabled) {
        this.isEnabled = enabled;
    }

    public boolean isFinalRankTree() {
        return this.isFinalRankTree;
    }

    public void setFinalRankTree(final boolean finalRankTree) {
        this.isFinalRankTree = finalRankTree;
    }

    public @NotNull IconSection getIcon() {
        return this.icon;
    }

    public @NotNull List<RRank> getRanks() {
        return Collections.unmodifiableList(this.ranks);
    }

    public void setRanks(final @NotNull List<RRank> ranks) {
        Objects.requireNonNull(ranks, "ranks cannot be null");
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
        return Collections.unmodifiableList(this.prerequisiteRankTrees);
    }

    public void setPrerequisiteRankTrees(final @NotNull List<RRankTree> prerequisiteRankTrees) {
        Objects.requireNonNull(prerequisiteRankTrees, "prerequisiteRankTrees cannot be null");
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
        return Collections.unmodifiableList(this.unlockedRankTrees);
    }

    public void setUnlockedRankTrees(final @NotNull List<RRankTree> unlockedRankTrees) {
        Objects.requireNonNull(unlockedRankTrees, "unlockedRankTrees cannot be null");
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
        return Collections.unmodifiableList(this.connectedRankTrees);
    }

    public void setConnectedRankTrees(final @NotNull List<RRankTree> connectedRankTrees) {
        Objects.requireNonNull(connectedRankTrees, "connectedRankTrees cannot be null");
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
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof final RRankTree other)) return false;
        return Objects.equals(this.identifier, other.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.identifier);
    }

    @Override
    public String toString() {
        return "RRankTree[identifier=%s, displayOrder=%d, enabled=%b]"
                .formatted(identifier, displayOrder, isEnabled);
    }
}