package com.raindropcentral.rdr.database.entity;

import java.util.Objects;

import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.raindropcentral.rplatform.database.converter.ItemStackConverter;

/**
 * Persistent banked progress for a single storage-store requirement token.
 *
 * <p>Each row belongs to exactly one {@link RDRPlayer} and stores either a banked currency amount,
 * a banked item stack, or both for a single requirement progress key. This keeps partial store
 * purchases normalized in their own table rather than embedding serialized progress maps on the
 * player record.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
@Entity
@Table(
    name = "rdr_store_requirement_progress",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_rdr_store_requirement_progress_player_key",
        columnNames = {"player_id", "progress_key"}
    )
)
/**
 * Represents the RStoreRequirementProgress API type.
 */
public class RStoreRequirementProgress extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private RDRPlayer player;

    @Column(name = "progress_key", nullable = false, length = 255)
    private String progressKey;

    @Column(name = "currency_amount", nullable = false)
    private double currencyAmount;

    @Convert(converter = ItemStackConverter.class)
    @Column(name = "item_stack", columnDefinition = "LONGTEXT")
    private ItemStack itemStack;

    /**
     * Creates a new progress row for the provided player and requirement token.
     *
     * @param player owning player entity
     * @param progressKey stable requirement progress token
     * @throws NullPointerException if {@code player} or {@code progressKey} is {@code null}
     * @throws IllegalArgumentException if {@code progressKey} is blank
     */
    public RStoreRequirementProgress(
        final @NotNull RDRPlayer player,
        final @NotNull String progressKey
    ) {
        this.progressKey = normalizeProgressKey(progressKey);
        Objects.requireNonNull(player, "player cannot be null").addStoreRequirementProgress(this);
    }

    /**
     * Constructor reserved for JPA entity hydration.
     */
    protected RStoreRequirementProgress() {}

    /**
     * Returns the owning player entity.
     *
     * @return owning player, or {@code null} only when detached in memory
     */
    public @Nullable RDRPlayer getPlayer() {
        return this.player;
    }

    /**
     * Returns the stable requirement progress token for this row.
     *
     * @return normalized progress key
     */
    public @NotNull String getProgressKey() {
        return this.progressKey;
    }

    /**
     * Updates the stable requirement progress token for this row.
     *
     * @param progressKey replacement token
     * @throws NullPointerException if {@code progressKey} is {@code null}
     * @throws IllegalArgumentException if {@code progressKey} is blank
     */
    public void setProgressKey(final @NotNull String progressKey) {
        this.progressKey = normalizeProgressKey(progressKey);
    }

    /**
     * Returns the currently banked currency amount.
     *
     * @return non-negative currency progress amount
     */
    public double getCurrencyAmount() {
        return this.currencyAmount;
    }

    /**
     * Updates the banked currency amount.
     *
     * @param currencyAmount replacement amount; negative values are clamped to {@code 0.0}
     */
    public void setCurrencyAmount(final double currencyAmount) {
        this.currencyAmount = Math.max(0.0D, currencyAmount);
    }

    /**
     * Returns the currently banked item stack.
     *
     * @return cloned banked item stack, or {@code null} when no item progress exists
     */
    public @Nullable ItemStack getItemStack() {
        return cloneItem(this.itemStack);
    }

    /**
     * Updates the banked item progress for this row.
     *
     * @param itemStack replacement item stack, or {@code null} to clear the item progress
     */
    public void setItemStack(final @Nullable ItemStack itemStack) {
        this.itemStack = cloneItem(itemStack);
    }

    /**
     * Returns whether this row currently stores any banked currency progress.
     *
     * @return {@code true} when the currency amount is positive
     */
    public boolean hasCurrencyProgress() {
        return this.currencyAmount > 0.0D;
    }

    /**
     * Returns whether this row currently stores any banked item progress.
     *
     * @return {@code true} when the item stack is non-empty
     */
    public boolean hasItemProgress() {
        return this.itemStack != null && !this.itemStack.isEmpty();
    }

    /**
     * Returns whether this row no longer carries any banked progress payload.
     *
     * @return {@code true} when neither currency nor item progress remains
     */
    public boolean isEmpty() {
        return !this.hasCurrencyProgress() && !this.hasItemProgress();
    }

    void setPlayerInternal(final @Nullable RDRPlayer player) {
        this.player = player;
    }

    private static @NotNull String normalizeProgressKey(final @NotNull String progressKey) {
        final String normalizedProgressKey = Objects.requireNonNull(progressKey, "progressKey cannot be null").trim();
        if (normalizedProgressKey.isEmpty()) {
            throw new IllegalArgumentException("progressKey cannot be blank");
        }
        return normalizedProgressKey;
    }

    private static @Nullable ItemStack cloneItem(final @Nullable ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return null;
        }
        return itemStack.clone();
    }
}
