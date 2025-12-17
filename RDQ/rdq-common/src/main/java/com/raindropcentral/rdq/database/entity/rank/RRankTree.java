package com.raindropcentral.rdq.database.entity.rank;

import com.raindropcentral.rdq.config.utility.IconSection;
import com.raindropcentral.rdq.database.converter.IconSectionConverter;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.Hibernate;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a tree of ranks within the RaindropQuests system.
 * <p>
 * A rank tree organizes a progression path for players, grouping related ranks and
 * defining prerequisites, connections, and unlock conditions between different trees.
 * Each tree has a unique identifier, display keys for localization, an icon, and
 * various configuration options for ordering and progression logic.
 * </p>
 *
 * <p>
 * This entity is mapped to the {@code r_rank_tree} table in the database.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.2
 * @since TBD
 */
@Entity
@Table(name = "r_rank_tree")
public class RRankTree extends BaseEntity {
	
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
	private boolean isEnabled;
	
	@Column(name = "is_final_rank_tree", nullable = false)
	private boolean isFinalRankTree;
	
	@Column(name = "icon", nullable = false, columnDefinition = "LONGTEXT")
	@Convert(converter = IconSectionConverter.class)
	private IconSection icon;
	
	@OneToMany(
		mappedBy = "rankTree",
		cascade = {CascadeType.PERSIST, CascadeType.MERGE},
		orphanRemoval = true,
		fetch = FetchType.LAZY
	)
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
	
	public RRankTree(
		final @NotNull String identifier,
		final @NotNull String displayNameKey,
		final @NotNull String descriptionKey,
		final @NotNull IconSection icon,
		final int displayOrder,
		final int minimumRankTreesToBeDone,
		final boolean isEnabled,
		final boolean isFinalRankTree
	) {
		this.identifier = identifier;
		this.displayNameKey = displayNameKey;
		this.descriptionKey = descriptionKey;
		this.icon = icon;
		this.displayOrder = displayOrder;
		this.minimumRankTreesToBeDone = minimumRankTreesToBeDone;
		this.isEnabled = isEnabled;
		this.isFinalRankTree = isFinalRankTree;
	}
	
	public String getIdentifier() {
		return this.identifier;
	}
	
	public String getDisplayNameKey() {
		return this.displayNameKey;
	}
	
	public String getDescriptionKey() {
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
	
	public boolean isFinalRankTree() {
		return this.isFinalRankTree;
	}
	
	public void setFinalRankTree(final boolean finalRankTree) {
		isFinalRankTree = finalRankTree;
	}
	
	public IconSection getIcon() {
		return this.icon;
	}
	
	public List<RRank> getRanks() {
		return this.ranks;
	}
	
	/**
	 * Sets the ranks for this rank tree.
	 * Handles lazy-loaded collections safely to avoid LazyInitializationException.
	 */
	public void setRanks(final List<RRank> ranks) {
		// Check if the collection is initialized to avoid lazy loading issues
		if (Hibernate.isInitialized(this.ranks)) {
			// Collection is initialized, safe to compare and modify
			if (!Objects.equals(this.ranks, ranks)) {
				this.ranks.clear();
				if (ranks != null) {
					this.ranks.addAll(ranks);
				}
			}
		} else {
			// Collection is not initialized, replace the entire collection
			this.ranks = ranks != null ? new ArrayList<>(ranks) : new ArrayList<>();
		}
	}
	
	public List<RRankTree> getPrerequisiteRankTrees() {
		return this.prerequisiteRankTrees;
	}
	
	/**
	 * Sets the prerequisite rank trees for this rank tree.
	 * Handles lazy-loaded collections safely to avoid LazyInitializationException.
	 */
	public void setPrerequisiteRankTrees(final List<RRankTree> prerequisiteRankTrees) {
		// Check if the collection is initialized to avoid lazy loading issues
		if (Hibernate.isInitialized(this.prerequisiteRankTrees)) {
			// Collection is initialized, safe to compare and modify
			if (!Objects.equals(this.prerequisiteRankTrees, prerequisiteRankTrees)) {
				this.prerequisiteRankTrees.clear();
				if (prerequisiteRankTrees != null) {
					this.prerequisiteRankTrees.addAll(prerequisiteRankTrees);
				}
			}
		} else {
			// Collection is not initialized, replace the entire collection
			this.prerequisiteRankTrees = prerequisiteRankTrees != null ? new ArrayList<>(prerequisiteRankTrees) : new ArrayList<>();
		}
	}
	
	public List<RRankTree> getUnlockedRankTrees() {
		return this.unlockedRankTrees;
	}
	
	/**
	 * Sets the unlocked rank trees for this rank tree.
	 * Handles lazy-loaded collections safely to avoid LazyInitializationException.
	 */
	public void setUnlockedRankTrees(final List<RRankTree> unlockedRankTrees) {
		// Check if the collection is initialized to avoid lazy loading issues
		if (Hibernate.isInitialized(this.unlockedRankTrees)) {
			// Collection is initialized, safe to compare and modify
			if (!Objects.equals(this.unlockedRankTrees, unlockedRankTrees)) {
				this.unlockedRankTrees.clear();
				if (unlockedRankTrees != null) {
					this.unlockedRankTrees.addAll(unlockedRankTrees);
				}
			}
		} else {
			// Collection is not initialized, replace the entire collection
			this.unlockedRankTrees = unlockedRankTrees != null ? new ArrayList<>(unlockedRankTrees) : new ArrayList<>();
		}
	}
	
	public List<RRankTree> getConnectedRankTrees() {
		return this.connectedRankTrees;
	}
	
	/**
	 * Sets the connected rank trees for this rank tree.
	 * Handles lazy-loaded collections safely to avoid LazyInitializationException.
	 */
	public void setConnectedRankTrees(final List<RRankTree> connectedRankTrees) {
		// Check if the collection is initialized to avoid lazy loading issues
		if (Hibernate.isInitialized(this.connectedRankTrees)) {
			// Collection is initialized, safe to compare and modify
			if (!Objects.equals(this.connectedRankTrees, connectedRankTrees)) {
				this.connectedRankTrees.clear();
				if (connectedRankTrees != null) {
					this.connectedRankTrees.addAll(connectedRankTrees);
				}
			}
		} else {
			// Collection is not initialized, replace the entire collection
			this.connectedRankTrees = connectedRankTrees != null ? new ArrayList<>(connectedRankTrees) : new ArrayList<>();
		}
	}
	
	public int getVersion() {
		return version;
	}
	
	// equals and hashCode based on identifier for entity identity
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof RRankTree)) return false;
		RRankTree that = (RRankTree) o;
		return Objects.equals(identifier, that.identifier);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(identifier);
	}
}