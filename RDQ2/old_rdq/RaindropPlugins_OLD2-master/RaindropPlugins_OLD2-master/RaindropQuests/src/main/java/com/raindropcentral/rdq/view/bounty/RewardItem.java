package com.raindropcentral.rdq.view.bounty;

import com.raindropcentral.rplatform.database.converter.ItemStackConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a reward item contributed by a player for a bounty.
 * <p>
 * This class is embeddable within JPA entities and stores information about the contributed item,
 * the contributor, the amount, and the time of contribution.
 * </p>
 *
 * <p>
 * The {@link ItemStack} is persisted using a custom converter to handle serialization.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
@Embeddable
public class RewardItem {
	
	/**
	 * The unique identifier for this reward item.
	 */
	@Column(
		name = "unique_id",
		nullable = false,
		unique = true
	)
	private UUID uniqueId;
	
	/**
	 * The item being contributed as a reward.
	 * <p>
	 * Serialized and stored in the database using {@link ItemStackConverter}.
	 * </p>
	 */
	@Column(
		name = "item",
		columnDefinition = "LONGTEXT",
		nullable = false
	)
	@Convert(converter = ItemStackConverter.class)
	private ItemStack item;
	
	/**
	 * The amount of the item being contributed.
	 */
	@Column(
		name = "amount",
		nullable = false
	)
	private int amount;
	
	/**
	 * The unique identifier of the player who contributed the item.
	 */
	@Column(
		name = "contributor_unique_id",
		nullable = false
	)
	private UUID contributorUniqueId;
	
	/**
	 * The timestamp when the item was contributed.
	 */
	@Column(
		name = "contributed_at",
		nullable = false
	)
	private LocalDateTime contributedAt;
	
	/**
	 * Protected no-args constructor for JPA.
	 */
	protected RewardItem() {}
	
	/**
	 * Constructs a new {@code RewardItem} with the specified item and contributor.
	 * <p>
	 * The unique ID is generated automatically, and the contribution time is set to the current time.
	 * </p>
	 *
	 * @param item        the {@link ItemStack} being contributed; must not be {@code null}
	 * @param contributor the {@link Player} contributing the item; must not be {@code null}
	 */
	public RewardItem(
		final @NotNull ItemStack item,
		final @NotNull Player contributor
	) {
		
		this.uniqueId = UUID.randomUUID();
		this.item = item;
		this.amount = item.getAmount();
		this.contributorUniqueId = contributor.getUniqueId();
		this.contributedAt = LocalDateTime.now();
	}
	
	/**
	 * Returns the contributed item.
	 *
	 * @return the {@link ItemStack} contributed
	 */
	public ItemStack getItem() {
		
		return this.item;
	}
	
	/**
	 * Sets the contributed item.
	 * <p>
	 * Intended for internal use or JPA only.
	 * </p>
	 *
	 * @param item the {@link ItemStack} to set
	 */
	protected void setItem(final ItemStack item) {
		
		this.item = item;
	}
	
	/**
	 * Returns the amount of the item contributed.
	 *
	 * @return the amount of the item
	 */
	public int getAmount() {
		
		return this.amount;
	}
	
	/**
	 * Sets the amount of the item contributed.
	 *
	 * @param amount the amount to set
	 */
	public void setAmount(final int amount) {
		
		this.amount = amount;
	}
	
	/**
	 * Returns the unique identifier of the contributor.
	 *
	 * @return the contributor's unique ID
	 */
	public UUID getContributorUniqueId() {
		
		return this.contributorUniqueId;
	}
	
	/**
	 * Sets the unique identifier of the contributor.
	 * <p>
	 * Intended for internal use or JPA only.
	 * </p>
	 *
	 * @param contributorUniqueId the contributor's unique ID to set
	 */
	protected void setContributorUniqueId(final UUID contributorUniqueId) {
		
		this.contributorUniqueId = contributorUniqueId;
	}
	
}