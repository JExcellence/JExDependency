/*
 * RDSPlayer.java
 *
 * @author ItsRainingHP
 * @version 5.0.0
 */

package com.raindropcentral.rds.database.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.raindropcentral.rplatform.database.converter.UUIDConverter;

/**
 * Persistent player extension for RDS shop-block ownership and shop-store progress.
 *
 * <p>Each player row tracks owned shop-block progression, whether the boss bar is enabled, and
 * which sidebar scoreboard type should be restored on join. It also stores normalized progress rows
 * used for partial shop-store requirement completion.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
@Entity
@Table(name = "rds_players")
@SuppressWarnings({
    "FieldCanBeLocal",
    "unused",
    "JpaDataSourceORMInspection"
})
public class RDSPlayer extends BaseEntity {

    @Column(name = "player_uuid", unique = true, nullable = false)
    @Convert(converter = UUIDConverter.class)
    private UUID player_uuid;

    @Column(name = "shops", nullable = false)
    private int shops;

    @Column(name = "shop_bar_enabled", nullable = false)
    private boolean shop_bar_enabled;

    @Column(name = "shop_sidebar_scoreboard_type")
    private String shop_sidebar_scoreboard_type;

    @OneToMany(
        mappedBy = "player",
        fetch = FetchType.EAGER,
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @OrderBy("progressKey ASC")
    private List<RShopStoreRequirementProgress> shopStoreRequirementProgress = new ArrayList<>();

    /**
     * Creates a player extension for the provided UUID.
     *
     * @param player_uuid unique player identifier
     */
    public RDSPlayer(final @NotNull UUID player_uuid) {
        this.player_uuid = Objects.requireNonNull(player_uuid, "player_uuid cannot be null");
        this.shops = 0;
        this.shop_bar_enabled = false;
        this.shop_sidebar_scoreboard_type = null;
    }

    /**
     * Constructor reserved for JPA entity hydration.
     */
    protected RDSPlayer() {}

    /**
     * Returns the cache identifier for this player entity.
     *
     * @return player UUID
     */
    public @NotNull UUID getIdentifier() {
        return this.player_uuid;
    }

    /**
     * Returns the number of owned shop blocks recorded for this player.
     *
     * @return owned shop-block count
     */
    public int getShops() {
        return this.shops;
    }

    /**
     * Returns whether the player has the shop boss bar enabled.
     *
     * @return {@code true} when the shop boss bar is enabled
     */
    public boolean isShopBarEnabled() {
        return this.shop_bar_enabled;
    }

    /**
     * Returns the saved sidebar scoreboard type for the player.
     *
     * <p>Legacy rows created before scoreboard persistence may not have a stored value.</p>
     *
     * @return saved scoreboard type, or an empty optional when no scoreboard should be restored
     */
    public @NotNull Optional<String> getShopSidebarScoreboardType() {
        return Optional.ofNullable(this.shop_sidebar_scoreboard_type)
            .map(String::trim)
            .filter(value -> !value.isEmpty());
    }

    /**
     * Returns whether the player has a sidebar scoreboard preference saved.
     *
     * @return {@code true} when a scoreboard type should be restored on join
     */
    public boolean hasShopSidebarScoreboard() {
        return this.getShopSidebarScoreboardType().isPresent();
    }

    /**
     * Toggles the shop boss bar state.
     *
     * @return the new enabled state
     */
    public boolean toggleShopBar() {
        this.shop_bar_enabled = !this.shop_bar_enabled;
        return this.shop_bar_enabled;
    }

    /**
     * Sets the shop boss bar state.
     *
     * @param enabled replacement enabled state
     */
    public void setShopBarEnabled(final boolean enabled) {
        this.shop_bar_enabled = enabled;
    }

    /**
     * Replaces the saved sidebar scoreboard type.
     *
     * @param scoreboardType replacement scoreboard type, or {@code null} to clear the preference
     */
    public void setShopSidebarScoreboardType(final @Nullable String scoreboardType) {
        this.shop_sidebar_scoreboard_type = normalizeScoreboardType(scoreboardType);
    }

    /**
     * Clears the saved sidebar scoreboard preference.
     */
    public void clearShopSidebarScoreboardType() {
        this.shop_sidebar_scoreboard_type = null;
    }

    /**
     * Increments the owned shop-block count.
     *
     * @param amount positive number of shop blocks to add
     */
    public void addShop(final int amount) {
        if (amount <= 0) {
            return;
        }

        this.shops += amount;
    }

    /**
     * Decrements the owned shop-block count.
     *
     * @param amount positive number of shop blocks to remove
     */
    public void removeShop(final int amount) {
        if (amount <= 0) {
            return;
        }

        this.shops = Math.max(0, this.shops - amount);
    }

    /**
     * Returns a defensive copy of the persisted banked item progress keyed by requirement token.
     *
     * @return copied item-progress map
     */
    public @NotNull Map<String, ItemStack> getStoreItemProgress() {
        final Map<String, ItemStack> itemProgress = new HashMap<>();
        for (final RShopStoreRequirementProgress progress : this.shopStoreRequirementProgress) {
            final ItemStack itemStack = progress.getItemStack();
            if (itemStack == null || itemStack.isEmpty()) {
                continue;
            }
            itemProgress.put(progress.getProgressKey(), itemStack);
        }
        return itemProgress;
    }

    /**
     * Returns the banked item progress for a requirement token.
     *
     * @param progressKey requirement progress token
     * @return cloned stored item stack, or {@code null} when none exists
     */
    public @Nullable ItemStack getStoreItemProgress(final @NotNull String progressKey) {
        return this.findShopStoreRequirementProgress(progressKey)
            .map(RShopStoreRequirementProgress::getItemStack)
            .orElse(null);
    }

    /**
     * Updates the banked item progress for a requirement token.
     *
     * @param progressKey requirement progress token
     * @param itemStack replacement banked stack, or {@code null} to clear the token
     */
    public void setStoreItemProgress(
        final @NotNull String progressKey,
        final @Nullable ItemStack itemStack
    ) {
        final String normalizedProgressKey = normalizeProgressKey(progressKey);
        final RShopStoreRequirementProgress progress = this.findShopStoreRequirementProgress(normalizedProgressKey)
            .orElseGet(() -> new RShopStoreRequirementProgress(this, normalizedProgressKey));
        progress.setItemStack(itemStack);
        this.removeProgressIfEmpty(progress);
    }

    /**
     * Returns the banked currency progress for a requirement token.
     *
     * @param progressKey requirement progress token
     * @return stored currency amount, or {@code 0.0} when no progress exists
     */
    public double getStoreCurrencyProgress(final @NotNull String progressKey) {
        return this.findShopStoreRequirementProgress(progressKey)
            .map(RShopStoreRequirementProgress::getCurrencyAmount)
            .orElse(0.0D);
    }

    /**
     * Returns a defensive copy of the persisted banked currency progress map.
     *
     * @return copied currency-progress map
     */
    public @NotNull Map<String, Double> getStoreCurrencyProgress() {
        final Map<String, Double> currencyProgress = new HashMap<>();
        for (final RShopStoreRequirementProgress progress : this.shopStoreRequirementProgress) {
            if (!progress.hasCurrencyProgress()) {
                continue;
            }
            currencyProgress.put(progress.getProgressKey(), progress.getCurrencyAmount());
        }
        return currencyProgress;
    }

    /**
     * Updates the banked currency progress for a requirement token.
     *
     * @param progressKey requirement progress token
     * @param amount replacement banked amount; non-positive values clear the token
     */
    public void setStoreCurrencyProgress(
        final @NotNull String progressKey,
        final double amount
    ) {
        final String normalizedProgressKey = normalizeProgressKey(progressKey);
        final RShopStoreRequirementProgress progress = this.findShopStoreRequirementProgress(normalizedProgressKey)
            .orElseGet(() -> new RShopStoreRequirementProgress(this, normalizedProgressKey));
        progress.setCurrencyAmount(amount);
        this.removeProgressIfEmpty(progress);
    }

    /**
     * Removes all stored progress entries matching a requirement prefix.
     *
     * @param progressKeyPrefix stable requirement progress prefix
     */
    public void clearStoreRequirementProgress(final @NotNull String progressKeyPrefix) {
        final String validatedPrefix = Objects.requireNonNull(progressKeyPrefix, "progressKeyPrefix cannot be null");
        final List<RShopStoreRequirementProgress> progressEntries = new ArrayList<>(this.shopStoreRequirementProgress);
        for (final RShopStoreRequirementProgress progress : progressEntries) {
            if (!progress.getProgressKey().startsWith(validatedPrefix)) {
                continue;
            }
            this.removeShopStoreRequirementProgress(progress);
        }
    }

    void addShopStoreRequirementProgress(final @NotNull RShopStoreRequirementProgress progress) {
        final RShopStoreRequirementProgress validatedProgress = Objects.requireNonNull(progress, "progress cannot be null");
        final RDSPlayer currentOwner = validatedProgress.getPlayer();
        if (currentOwner == this && this.containsShopStoreRequirementProgress(validatedProgress)) {
            return;
        }
        if (currentOwner != null && currentOwner != this) {
            currentOwner.removeShopStoreRequirementProgress(validatedProgress);
        }
        if (!this.containsShopStoreRequirementProgress(validatedProgress)) {
            this.shopStoreRequirementProgress.add(validatedProgress);
        }
        validatedProgress.setPlayerInternal(this);
    }

    void removeShopStoreRequirementProgress(final @NotNull RShopStoreRequirementProgress progress) {
        final RShopStoreRequirementProgress validatedProgress = Objects.requireNonNull(progress, "progress cannot be null");
        if (this.shopStoreRequirementProgress.removeIf(
            existingProgress -> this.matchesShopStoreRequirementProgress(existingProgress, validatedProgress)
        )) {
            validatedProgress.setPlayerInternal(null);
        }
    }

    private boolean containsShopStoreRequirementProgress(final @NotNull RShopStoreRequirementProgress candidate) {
        return this.shopStoreRequirementProgress.stream()
            .anyMatch(existingProgress -> this.matchesShopStoreRequirementProgress(existingProgress, candidate));
    }

    private boolean matchesShopStoreRequirementProgress(
        final @NotNull RShopStoreRequirementProgress left,
        final @NotNull RShopStoreRequirementProgress right
    ) {
        if (left == right) {
            return true;
        }

        final Long leftId = left.getId();
        final Long rightId = right.getId();
        if (leftId != null && Objects.equals(leftId, rightId)) {
            return true;
        }

        return left.getProgressKey().equals(right.getProgressKey());
    }

    private void removeProgressIfEmpty(final @NotNull RShopStoreRequirementProgress progress) {
        if (progress.isEmpty()) {
            this.removeShopStoreRequirementProgress(progress);
        }
    }

    private @NotNull Optional<RShopStoreRequirementProgress> findShopStoreRequirementProgress(
        final @NotNull String progressKey
    ) {
        final String normalizedProgressKey = normalizeProgressKey(progressKey);
        return this.shopStoreRequirementProgress.stream()
            .filter(progress -> progress.getProgressKey().equals(normalizedProgressKey))
            .findFirst();
    }

    private static @NotNull String normalizeProgressKey(final @NotNull String progressKey) {
        final String normalizedProgressKey = Objects.requireNonNull(progressKey, "progressKey cannot be null").trim();
        if (normalizedProgressKey.isEmpty()) {
            throw new IllegalArgumentException("progressKey cannot be blank");
        }
        return normalizedProgressKey;
    }

    private static @Nullable String normalizeScoreboardType(final @Nullable String scoreboardType) {
        if (scoreboardType == null) {
            return null;
        }

        final String normalizedScoreboardType = scoreboardType.trim().toLowerCase(Locale.ROOT);
        return normalizedScoreboardType.isEmpty() ? null : normalizedScoreboardType;
    }
}