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

/**
 * Represents a rank tree configuration that defines the relationships between ranks and the
 * requirements needed to unlock subsequent trees. This entity is persisted in the
 * {@code r_rank_tree} table and keeps track of icon metadata, ordering rules, and state flags.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
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

    /**
     * Framework-only constructor required by JPA.
     */
    protected RRankTree() {}

    /**
     * Creates a new rank tree definition.
     *
     * @param identifier                 unique identifier for the rank tree.
     * @param displayNameKey             translation key used for the display name.
     * @param descriptionKey             translation key used for the description text.
     * @param icon                       icon metadata rendered for the tree.
     * @param displayOrder               order in which the tree is displayed in menus.
     * @param minimumRankTreesToBeDone   minimum amount of trees the player must complete.
     * @param isEnabled                  indicates whether the tree is enabled.
     * @param isFinalRankTree            indicates whether the tree is final in its branch.
     */
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

    /**
     * Retrieves the unique identifier of the rank tree.
     *
     * @return the non-null identifier.
     */
    public @NotNull String getIdentifier() {
        return this.identifier;
    }

    /**
     * Retrieves the translation key for the display name.
     *
     * @return the non-null display name key.
     */
    public @NotNull String getDisplayNameKey() {
        return this.displayNameKey;
    }

    /**
     * Retrieves the translation key for the description.
     *
     * @return the non-null description key.
     */
    public @NotNull String getDescriptionKey() {
        return this.descriptionKey;
    }

    /**
     * Gets the display order used to sort rank trees.
     *
     * @return the configured display order.
     */
    public int getDisplayOrder() {
        return this.displayOrder;
    }

    /**
     * Updates the display order used to sort rank trees.
     *
     * @param displayOrder the new display order.
     */
    public void setDisplayOrder(final int displayOrder) {
        this.displayOrder = displayOrder;
    }

    /**
     * Retrieves the minimum number of rank trees required for completion.
     *
     * @return the minimum number of rank trees to be completed.
     */
    public int getMinimumRankTreesToBeDone() {
        return this.minimumRankTreesToBeDone;
    }

    /**
     * Updates the minimum number of rank trees required for completion.
     *
     * @param minimumRankTreesToBeDone the new minimum count of completed rank trees.
     */
    public void setMinimumRankTreesToBeDone(final int minimumRankTreesToBeDone) {
        this.minimumRankTreesToBeDone = minimumRankTreesToBeDone;
    }

    /**
     * Indicates whether the rank tree is enabled.
     *
     * @return {@code true} if the rank tree is enabled; {@code false} otherwise.
     */
    public boolean isEnabled() {
        return this.isEnabled;
    }

    /**
     * Sets whether the rank tree is enabled.
     *
     * @param enabled {@code true} to enable the tree; {@code false} to disable it.
     */
    public void setEnabled(final boolean enabled) {
        this.isEnabled = enabled;
    }

    /**
     * Indicates whether this rank tree represents the final tree in its branch.
     *
     * @return {@code true} if the tree is the final one; {@code false} otherwise.
     */
    public boolean isFinalRankTree() {
        return this.isFinalRankTree;
    }

    /**
     * Sets whether this rank tree is considered the final tree.
     *
     * @param finalRankTree {@code true} to flag the tree as final; {@code false} otherwise.
     */
    public void setFinalRankTree(final boolean finalRankTree) {
        this.isFinalRankTree = finalRankTree;
    }

    /**
     * Retrieves the icon metadata associated with the rank tree.
     *
     * @return the non-null icon section.
     */
    public @NotNull IconSection getIcon() {
        return this.icon;
    }

    /**
     * Provides an immutable view of ranks configured under this tree.
     *
     * @return an unmodifiable list of ranks.
     */
    public @NotNull List<RRank> getRanks() {
        return Collections.unmodifiableList(this.ranks);
    }

    /**
     * Replaces the ranks associated with this tree.
     *
     * @param ranks the new non-null list of ranks.
     */
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

    /**
     * Provides the trees that must be completed before this one.
     *
     * @return an unmodifiable list of prerequisite rank trees.
     */
    public @NotNull List<RRankTree> getPrerequisiteRankTrees() {
        return Collections.unmodifiableList(this.prerequisiteRankTrees);
    }

    /**
     * Updates the prerequisite rank trees that gate access to this tree.
     *
     * @param prerequisiteRankTrees the new non-null list of prerequisite trees.
     */
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

    /**
     * Provides the trees that become available after completing this tree.
     *
     * @return an unmodifiable list of unlocked rank trees.
     */
    public @NotNull List<RRankTree> getUnlockedRankTrees() {
        return Collections.unmodifiableList(this.unlockedRankTrees);
    }

    /**
     * Updates the trees that this tree unlocks upon completion.
     *
     * @param unlockedRankTrees the new non-null list of unlocked trees.
     */
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

    /**
     * Provides the trees that are visually or logically connected to this tree.
     *
     * @return an unmodifiable list of connected rank trees.
     */
    public @NotNull List<RRankTree> getConnectedRankTrees() {
        return Collections.unmodifiableList(this.connectedRankTrees);
    }

    /**
     * Updates the trees that are connected to this tree within the UI graph.
     *
     * @param connectedRankTrees the new non-null list of connected trees.
     */
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

    /**
     * Retrieves the optimistic locking version of the entity.
     *
     * @return the current entity version.
     */
    public int getVersion() {
        return this.version;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof final RRankTree other)) return false;
        return Objects.equals(this.identifier, other.identifier);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.identifier);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "RRankTree[identifier=%s, displayOrder=%d, enabled=%b]"
                .formatted(identifier, displayOrder, isEnabled);
    }
}
