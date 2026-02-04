package com.raindropcentral.rdq.database.entity.rank;

import com.raindropcentral.rdq.config.utility.IconSection;
import com.raindropcentral.rdq.database.converter.IconSectionConverter;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Represents a rank within a {@link RRankTree} in the RaindropQuests system.
 * <p>
 * Each rank has a unique identifier, display keys for localization, a LuckPerms group assignment,
 * tier and weight for ordering, and icon representation. Ranks can be linked to previous and next ranks,
 * forming a progression path within a rank tree.
 * </p>
 *
 * <p>
 * This entity is mapped to the {@code r_rank} table in the database.
 * </p>
 *
 * @author JExcellence
 * @version 2.0.0
 * @since TBD
 */
@Entity
@Table(name = "r_rank")
public class RRank extends BaseEntity {
	
	@Transient
	private static final Logger LOGGER = Logger.getLogger(RRank.class.getName());
	
	@Column(
		name = "identifier",
		nullable = false,
		unique = true
	)
	private String identifier;
	
	@Column(
		name = "display_name_key",
		nullable = false,
		unique = true
	)
	private String displayNameKey;
	
	@Column(
		name = "description_key",
		nullable = false
	)
	private String descriptionKey;
	
	@Column(
		name = "assigned_luckperms_group",
		nullable = false
	)
	private String assignedLuckPermsGroup;
	
	@Column(
		name = "prefix_key",
		nullable = false
	)
	private String prefixKey;
	
	@Column(
		name = "suffix_key",
		nullable = false
	)
	private String suffixKey;
	
	@Column(
		name = "tier",
		nullable = false
	)
	private int tier;
	
	@Column(
		name = "weight",
		nullable = false
	)
	private int weight;
	
	@Column(
		name = "is_initial_rank"
	)
	private boolean isInitialRank;
	
	@Column(
		name = "is_final_rank"
	)
	private boolean isFinalRank;
	
	@Column(
		name = "is_enabled"
	)
	private boolean isEnabled;
	
	@Column(
		name = "icon",
		nullable = false,
		columnDefinition = "LONGTEXT"
	)
	@Convert(converter = IconSectionConverter.class)
	private IconSection icon;
	
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(
		name = "r_rank_previous_ranks",
		joinColumns = @JoinColumn(
			name = "rank_id"
		)
	)
	@Column(
		name = "previous_rank_identifier"
	)
	private List<String> previousRanks = new ArrayList<>();
	
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(
		name = "r_rank_next_ranks",
		joinColumns = @JoinColumn(
			name = "rank_id"
		)
	)
	@Column(
		name = "next_rank_identifier"
	)
	private List<String> nextRanks = new ArrayList<>();
	
	/**
	 * The {@link RRankTree} to which this rank belongs.
	 * For the default rank, this may be null.
	 */
	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "rank_tree_id", nullable = true)
	private RRankTree rankTree;
	
	/**
	 * The upgrade requirements for this rank.
	 * Each requirement represents a different requirement that must be completed for upgrading to this rank.
	 */
	@OneToMany(
		mappedBy = "rank",
		cascade = CascadeType.ALL,
		orphanRemoval = true,
		fetch = FetchType.EAGER
	)
	private Set<RRankUpgradeRequirement> upgradeRequirements = new HashSet<>();
	
	/**
	 * The rewards granted when achieving this rank.
	 */
	@OneToMany(
		mappedBy = "rank",
		cascade = CascadeType.ALL,
		orphanRemoval = true,
		fetch = FetchType.EAGER
	)
	private Set<RRankReward> rewards = new HashSet<>();
	
	/**
	 * Optimistic locking version field.
	 */
	@Version
	@Column(name = "version")
	private int version;
	
	/**
	 * Protected no-args constructor for JPA.
	 */
	protected RRank() {}
	
	/**
	 * Constructs a new {@code RRank} with the specified properties.
	 * For the default rank, rankTree may be null.
	 *
	 * @param identifier             the unique identifier for the rank
	 * @param displayNameKey         the localization key for the display name
	 * @param descriptionKey         the localization key for the description
	 * @param assignedLuckPermsGroup the LuckPerms group assigned to this rank
	 * @param prefixKey              the localization key for the prefix
	 * @param suffixKey              the localization key for the suffix
	 * @param icon                   the icon for this rank
	 * @param tier                   the tier of the rank
	 * @param weight                 the weight of the rank
	 * @param rankTree               the rank tree this rank belongs to (nullable for default rank)
	 */
	public RRank(
		final @NotNull String identifier,
		final @NotNull String displayNameKey,
		final @NotNull String descriptionKey,
		final @NotNull String assignedLuckPermsGroup,
		final @NotNull String prefixKey,
		final @NotNull String suffixKey,
		final @NotNull IconSection icon,
		final boolean isInitialRank,
		final int tier,
		final int weight,
		final @Nullable RRankTree rankTree
	) {
		this.identifier = identifier;
		this.displayNameKey = displayNameKey;
		this.descriptionKey = descriptionKey;
		this.assignedLuckPermsGroup = assignedLuckPermsGroup;
		this.prefixKey = prefixKey;
		this.suffixKey = suffixKey;
		this.icon = icon;
		this.isInitialRank = isInitialRank;
		this.tier = tier;
		this.weight = weight;
		this.rankTree = rankTree;
	}
	
	/**
	 * Legacy constructor for compatibility (without rankTree, for default rank).
	 */
	public RRank(
		final @NotNull String identifier,
		final @NotNull String displayNameKey,
		final @NotNull String descriptionKey,
		final @NotNull String assignedLuckPermsGroup,
		final @NotNull String prefixKey,
		final @NotNull String suffixKey,
		final @NotNull IconSection icon,
		final boolean isInitialRank,
		final int tier,
		final int weight
	) {
		this(identifier, displayNameKey, descriptionKey, assignedLuckPermsGroup, prefixKey, suffixKey, icon, isInitialRank, tier, weight, null);
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
	
	public String getAssignedLuckPermsGroup() {
		return this.assignedLuckPermsGroup;
	}
	
	public String getPrefixKey() {
		return this.prefixKey;
	}
	
	public String getSuffixKey() {
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
	
	/**
	 * Gets the rank tree this rank belongs to, or null if this is the default rank.
	 *
	 * @return the rank tree, or null
	 */
	@Nullable
	public RRankTree getRankTree() {
		return this.rankTree;
	}
	
	/**
	 * Gets the upgrade requirements for this rank.
	 *
	 * @return a set of upgrade requirements
	 */
	@NotNull
	public Set<RRankUpgradeRequirement> getUpgradeRequirements() {
		return this.upgradeRequirements;
	}
	
	/**
	 * Gets upgrade requirements ordered by display order.
	 *
	 * @return a list of upgrade requirements sorted by display order
	 */
	@NotNull
	public List<RRankUpgradeRequirement> getUpgradeRequirementsOrdered() {
		return this.upgradeRequirements.stream()
		                               .sorted((r1, r2) -> Integer.compare(r1.getDisplayOrder(), r2.getDisplayOrder()))
		                               .collect(Collectors.toList());
	}
	
	/**
	 * Gets the rewards for this rank.
	 *
	 * @return a set of rewards
	 */
	@NotNull
	public Set<RRankReward> getRewards() {
		return this.rewards;
	}
	
	/**
	 * Gets rewards ordered by display order.
	 *
	 * @return a list of rewards sorted by display order
	 */
	@NotNull
	public List<RRankReward> getRewardsOrdered() {
		return this.rewards.stream()
		                   .sorted((r1, r2) -> Integer.compare(r1.getDisplayOrder(), r2.getDisplayOrder()))
		                   .collect(Collectors.toList());
	}
	
	public IconSection getIcon() {
		return this.icon;
	}
	
	public List<String> getPreviousRanks() {
		return this.previousRanks;
	}
	
	public List<String> getNextRanks() {
		return this.nextRanks;
	}
	
	public int getVersion() {
		return version;
	}
	
	public void setRankTree(final @Nullable RRankTree rankTree) {
		this.rankTree = rankTree;
	}
	
	public void setPreviousRanks(final List<String> previousRanks) {
		this.previousRanks = previousRanks;
	}
	
	public void setNextRanks(final List<String> nextRanks) {
		this.nextRanks = nextRanks;
	}
	
	/**
	 * Adds an upgrade requirement to this rank.
	 * Properly manages the bidirectional relationship and prevents duplicates.
	 *
	 * @param upgradeRequirement the upgrade requirement to add
	 * @return true if the requirement was added, false if it was already present
	 */
	public boolean addUpgradeRequirement(@NotNull final RRankUpgradeRequirement upgradeRequirement) {
		
		if (this.upgradeRequirements.contains(upgradeRequirement)) {
			LOGGER.log(Level.FINE, "Upgrade requirement already exists for rank: " + this.identifier);
			return false;
		}
		
		boolean added = this.upgradeRequirements.add(upgradeRequirement);
		
		if (added) {
			if (upgradeRequirement.getRank() != this) {
				upgradeRequirement.setRank(this);
			}
			
			LOGGER.log(Level.FINE, "Added upgrade requirement to rank: " + this.identifier +
			                       ". Total requirements: " + this.upgradeRequirements.size());
		}
		
		return added;
	}
	
	/**
	 * Removes an upgrade requirement from this rank.
	 * Properly manages the bidirectional relationship.
	 *
	 * @param upgradeRequirement the upgrade requirement to remove
	 * @return true if the requirement was removed, false if it wasn't present
	 */
	public boolean removeUpgradeRequirement(@NotNull final RRankUpgradeRequirement upgradeRequirement) {
		
		boolean removed = this.upgradeRequirements.remove(upgradeRequirement);
		
		if (removed) {
			if (upgradeRequirement.getRank() == this) {
				upgradeRequirement.setRank(null);
			}
			
			LOGGER.log(
				Level.FINE, "Removed upgrade requirement from rank: " + this.identifier +
				            ". Remaining requirements: " + this.upgradeRequirements.size());
		}
		
		return removed;
	}
	
	/**
	 * Adds a reward to this rank.
	 * Properly manages the bidirectional relationship and prevents duplicates.
	 *
	 * @param reward the reward to add
	 * @return true if the reward was added, false if it was already present
	 */
	public boolean addReward(@NotNull final RRankReward reward) {
		if (this.rewards.contains(reward)) {
			LOGGER.log(Level.FINE, "Reward already exists for rank: " + this.identifier);
			return false;
		}
		
		boolean added = this.rewards.add(reward);
		
		if (added) {
			if (reward.getRank() != this) {
				reward.setRank(this);
			}
			
			LOGGER.log(Level.FINE, "Added reward to rank: " + this.identifier +
			                       ". Total rewards: " + this.rewards.size());
		}
		
		return added;
	}
	
	/**
	 * Removes a reward from this rank.
	 * Properly manages the bidirectional relationship.
	 *
	 * @param reward the reward to remove
	 * @return true if the reward was removed, false if it wasn't present
	 */
	public boolean removeReward(@NotNull final RRankReward reward) {
		boolean removed = this.rewards.remove(reward);
		
		if (removed) {
			if (reward.getRank() == this) {
				reward.setRank(null);
			}
			
			LOGGER.log(Level.FINE, "Removed reward from rank: " + this.identifier +
			                      ". Remaining rewards: " + this.rewards.size());
		}
		
		return removed;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof final RRank rRank)) return false;
		return identifier.equals(rRank.identifier);
	}
	
	@Override
	public int hashCode() {
		return identifier.hashCode();
	}
}