package com.raindropcentral.rdq.database.entity.reward;

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
 * Represents a single item that was contributed to satisfy a bounty reward.
 * <p>
 * The embeddable structure captures the contributed {@link ItemStack}, the player who supplied it
 * and bookkeeping details such as the quantity and contribution timestamp. The {@link ItemStack}
 * payload is serialized through {@link ItemStackConverter} so it can be stored in the database.
 * </p>
 *
 * <p>
 * Instances of this class are typically created when a player turns in items for a bounty and are
 * later persisted as part of the owning bounty entity.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.1
 * @since 1.0.0
 */
@Embeddable
public class RewardItem {

    /**
     * The unique identifier for this reward item entry.
     */
    @Column(
            name = "unique_id",
            nullable = false,
            unique = true
    )
    private UUID uniqueId;

    /**
     * The item being contributed as a reward. Serialized with {@link ItemStackConverter}.
     */
    @Column(
            name = "item",
            columnDefinition = "LONGTEXT",
            nullable = false
    )
    @Convert(converter = ItemStackConverter.class)
    private ItemStack item;

    /**
     * The quantity of the contributed item stack.
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
     * The timestamp recorded when the contribution was made.
     */
    @Column(
            name = "contributed_at",
            nullable = false
    )
    private LocalDateTime contributedAt;

    /**
     * Creates an instance for frameworks that require a no-argument constructor.
     * <p>
     * This constructor should not be used directly in application code.
     * </p>
     */
    protected RewardItem() {
    }

    /**
     * Creates a {@link RewardItem} for scenarios where persistence frameworks populate the
     * contributor metadata separately.
     * <p>
     * The contributor identifier and contribution timestamp remain unset to allow the owning entity
     * to supply those values when they are known.
     * </p>
     *
     * @param item the {@link ItemStack} that was contributed; must not be {@code null}
     */
    public RewardItem(final @NotNull ItemStack item) {
        this.uniqueId = null;
        this.item = item;
        this.amount = item.getAmount();
        this.contributorUniqueId = null;
        this.contributedAt = null;
    }

    /**
     * Creates a fully populated {@link RewardItem} from a live player contribution.
     *
     * @param item        the {@link ItemStack} being contributed; must not be {@code null}
     * @param contributor the {@link Player} submitting the contribution; must not be {@code null}
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
     * Returns the contributed item stack.
     *
     * @return the persisted {@link ItemStack}
     */
    public ItemStack getItem() {
        return this.item;
    }

    /**
     * Replaces the contributed item stack.
     * <p>
     * Primarily intended for JPA or other persistence mechanisms.
     * </p>
     *
     * @param item the new {@link ItemStack} value; must not be {@code null}
     */
    protected void setItem(final ItemStack item) {
        this.item = item;
    }

    /**
     * Returns the amount of the contributed item stack.
     *
     * @return the contribution quantity
     */
    public int getAmount() {
        return this.amount;
    }

    /**
     * Updates the tracked amount for the contributed item stack.
     *
     * @param amount the new quantity to record
     */
    public void setAmount(final int amount) {
        this.amount = amount;
    }

    /**
     * Returns the UUID of the contributing player.
     *
     * @return the contributor's unique identifier, or {@code null} if not yet assigned
     */
    public UUID getContributorUniqueId() {
        return this.contributorUniqueId;
    }

    /**
     * Updates the UUID of the contributing player.
     * <p>
     * Intended for framework usage when rehydrating data.
     * </p>
     *
     * @param contributorUniqueId the contributor's unique identifier
     */
    protected void setContributorUniqueId(final UUID contributorUniqueId) {
        this.contributorUniqueId = contributorUniqueId;
    }
}
