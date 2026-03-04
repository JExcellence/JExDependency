package com.raindropcentral.rdr.database.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
 * Persistent player extension for RDR storage and store-progress data.
 *
 * <p>Each player row owns zero or more {@link RStorage} records representing individual vaults or
 * other inventory-backed containers, plus normalized {@link RStoreRequirementProgress} rows used to
 * persist partial progress for storage-store purchases. The entity also stores whether the player
 * wants the storage sidebar scoreboard restored automatically on login. The relationships cascade
 * persistence operations so player-level updates keep child rows in sync.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
@Entity
@Table(name = "rdr_players")
public class RDRPlayer extends BaseEntity {

    @Column(name = "player_uuid", unique = true, nullable = false)
    @Convert(converter = UUIDConverter.class)
    private UUID playerUuid;

    @Column(name = "sidebar_scoreboard_enabled")
    private Boolean sidebarScoreboardEnabled;

    @OneToMany(
        mappedBy = "player",
        fetch = FetchType.EAGER,
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @OrderBy("storageKey ASC")
    private List<RStorage> storages = new ArrayList<>();

    @OneToMany(
        mappedBy = "player",
        fetch = FetchType.EAGER,
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @OrderBy("progressKey ASC")
    private List<RStoreRequirementProgress> storeRequirementProgress = new ArrayList<>();

    /**
     * Creates a persisted player extension for the provided UUID.
     *
     * @param playerUuid unique player identifier used as the repository key
     * @throws NullPointerException if {@code playerUuid} is {@code null}
     */
    public RDRPlayer(final @NotNull UUID playerUuid) {
        this.playerUuid = Objects.requireNonNull(playerUuid, "playerUuid cannot be null");
        this.sidebarScoreboardEnabled = false;
    }

    /**
     * Constructor reserved for JPA entity hydration.
     */
    protected RDRPlayer() {}

    /**
     * Returns the unique player identifier used by the repository cache.
     *
     * @return persisted player UUID
     */
    public @NotNull UUID getIdentifier() {
        return this.playerUuid;
    }

    /**
     * Returns whether the player has the storage sidebar scoreboard enabled.
     *
     * <p>The value may be {@code null} for legacy rows created before the preference existed.</p>
     *
     * @return {@code true} when the scoreboard should be restored automatically
     */
    public boolean isSidebarScoreboardEnabled() {
        return Boolean.TRUE.equals(this.sidebarScoreboardEnabled);
    }

    /**
     * Toggles the persisted storage sidebar scoreboard preference.
     *
     * @return the new enabled state after toggling
     */
    public boolean toggleSidebarScoreboard() {
        final boolean enabled = !this.isSidebarScoreboardEnabled();
        this.sidebarScoreboardEnabled = enabled;
        return enabled;
    }

    /**
     * Replaces the persisted storage sidebar scoreboard preference.
     *
     * @param enabled replacement enabled state
     */
    public void setSidebarScoreboardEnabled(final boolean enabled) {
        this.sidebarScoreboardEnabled = enabled;
    }

    /**
     * Returns the player's storage containers as an immutable snapshot.
     *
     * @return immutable view of associated storage entities
     */
    public @NotNull List<RStorage> getStorages() {
        return List.copyOf(this.storages);
    }

    /**
     * Finds a storage owned by this player using its logical storage key.
     *
     * @param storageKey storage key to match
     * @return matching storage when present
     * @throws NullPointerException if {@code storageKey} is {@code null}
     */
    public @NotNull Optional<RStorage> findStorage(final @NotNull String storageKey) {
        final String normalizedStorageKey = Objects.requireNonNull(storageKey, "storageKey cannot be null").trim();
        return this.storages.stream()
            .filter(storage -> storage.getStorageKey().equals(normalizedStorageKey))
            .findFirst();
    }

    /**
     * Associates the provided storage with this player and keeps the bidirectional relationship in sync.
     *
     * @param storage storage to associate with this player
     * @throws NullPointerException if {@code storage} is {@code null}
     */
    public void addStorage(final @NotNull RStorage storage) {
        final RStorage validatedStorage = Objects.requireNonNull(storage, "storage cannot be null");
        final RDRPlayer currentOwner = validatedStorage.getPlayer();
        if (currentOwner == this && this.containsStorage(validatedStorage)) {
            return;
        }
        if (currentOwner != null && currentOwner != this) {
            currentOwner.removeStorage(validatedStorage);
        }
        if (!this.containsStorage(validatedStorage)) {
            this.storages.add(validatedStorage);
        }
        validatedStorage.setPlayerInternal(this);
    }

    /**
     * Removes the provided storage from this player.
     *
     * @param storage storage to detach
     * @throws NullPointerException if {@code storage} is {@code null}
     */
    public void removeStorage(final @NotNull RStorage storage) {
        final RStorage validatedStorage = Objects.requireNonNull(storage, "storage cannot be null");
        if (this.storages.removeIf(existingStorage -> this.matchesStorage(existingStorage, validatedStorage))) {
            validatedStorage.setPlayerInternal(null);
        }
    }

    /**
     * Returns a defensive copy of the persisted banked item progress keyed by requirement progress token.
     *
     * @return copied item-progress map
     */
    public @NotNull Map<String, ItemStack> getStoreItemProgress() {
        final Map<String, ItemStack> itemProgress = new HashMap<>();
        for (final RStoreRequirementProgress progress : this.storeRequirementProgress) {
            final ItemStack itemStack = progress.getItemStack();
            if (itemStack == null || itemStack.isEmpty()) {
                continue;
            }
            itemProgress.put(progress.getProgressKey(), itemStack);
        }
        return itemProgress;
    }

    /**
     * Returns the banked item progress for a specific requirement progress token.
     *
     * @param progressKey requirement progress token to resolve
     * @return cloned stored item stack, or {@code null} when no banked progress exists
     * @throws NullPointerException if {@code progressKey} is {@code null}
     */
    public @Nullable ItemStack getStoreItemProgress(final @NotNull String progressKey) {
        return this.findStoreRequirementProgress(progressKey)
            .map(RStoreRequirementProgress::getItemStack)
            .orElse(null);
    }

    /**
     * Updates the banked item progress for a specific requirement progress token.
     *
     * @param progressKey requirement progress token to replace
     * @param itemStack replacement banked item stack, or {@code null} to clear the token
     * @throws NullPointerException if {@code progressKey} is {@code null}
     */
    public void setStoreItemProgress(
        final @NotNull String progressKey,
        final @Nullable ItemStack itemStack
    ) {
        final String normalizedProgressKey = normalizeProgressKey(progressKey);
        final RStoreRequirementProgress progress = this.findStoreRequirementProgress(normalizedProgressKey)
            .orElseGet(() -> new RStoreRequirementProgress(this, normalizedProgressKey));
        progress.setItemStack(itemStack);
        this.removeProgressIfEmpty(progress);
    }

    /**
     * Returns the banked currency progress for a specific requirement progress token.
     *
     * @param progressKey requirement progress token to resolve
     * @return stored currency amount, or {@code 0.0} when no progress exists
     * @throws NullPointerException if {@code progressKey} is {@code null}
     */
    public double getStoreCurrencyProgress(final @NotNull String progressKey) {
        return this.findStoreRequirementProgress(progressKey)
            .map(RStoreRequirementProgress::getCurrencyAmount)
            .orElse(0.0D);
    }

    /**
     * Returns a defensive copy of the persisted banked currency progress keyed by requirement progress token.
     *
     * @return copied currency-progress map
     */
    public @NotNull Map<String, Double> getStoreCurrencyProgress() {
        final Map<String, Double> currencyProgress = new HashMap<>();
        for (final RStoreRequirementProgress progress : this.storeRequirementProgress) {
            if (!progress.hasCurrencyProgress()) {
                continue;
            }
            currencyProgress.put(progress.getProgressKey(), progress.getCurrencyAmount());
        }
        return currencyProgress;
    }

    /**
     * Updates the banked currency progress for a specific requirement progress token.
     *
     * @param progressKey requirement progress token to replace
     * @param amount replacement banked amount; non-positive values clear the token
     * @throws NullPointerException if {@code progressKey} is {@code null}
     */
    public void setStoreCurrencyProgress(
        final @NotNull String progressKey,
        final double amount
    ) {
        final String normalizedProgressKey = normalizeProgressKey(progressKey);
        final RStoreRequirementProgress progress = this.findStoreRequirementProgress(normalizedProgressKey)
            .orElseGet(() -> new RStoreRequirementProgress(this, normalizedProgressKey));
        progress.setCurrencyAmount(amount);
        this.removeProgressIfEmpty(progress);
    }

    /**
     * Removes every stored item and currency progress entry that belongs to a requirement token prefix.
     *
     * @param progressKeyPrefix stable requirement progress prefix to clear
     * @throws NullPointerException if {@code progressKeyPrefix} is {@code null}
     */
    public void clearStoreRequirementProgress(final @NotNull String progressKeyPrefix) {
        final String validatedPrefix = Objects.requireNonNull(progressKeyPrefix, "progressKeyPrefix cannot be null");
        final List<RStoreRequirementProgress> progressEntries = new ArrayList<>(this.storeRequirementProgress);
        for (final RStoreRequirementProgress progress : progressEntries) {
            if (!progress.getProgressKey().startsWith(validatedPrefix)) {
                continue;
            }
            this.removeStoreRequirementProgress(progress);
        }
    }

    void addStoreRequirementProgress(final @NotNull RStoreRequirementProgress progress) {
        final RStoreRequirementProgress validatedProgress = Objects.requireNonNull(progress, "progress cannot be null");
        final RDRPlayer currentOwner = validatedProgress.getPlayer();
        if (currentOwner == this && this.containsStoreRequirementProgress(validatedProgress)) {
            return;
        }
        if (currentOwner != null && currentOwner != this) {
            currentOwner.removeStoreRequirementProgress(validatedProgress);
        }
        if (!this.containsStoreRequirementProgress(validatedProgress)) {
            this.storeRequirementProgress.add(validatedProgress);
        }
        validatedProgress.setPlayerInternal(this);
    }

    void removeStoreRequirementProgress(final @NotNull RStoreRequirementProgress progress) {
        final RStoreRequirementProgress validatedProgress = Objects.requireNonNull(progress, "progress cannot be null");
        if (this.storeRequirementProgress.removeIf(existingProgress -> this.matchesStoreRequirementProgress(existingProgress, validatedProgress))) {
            validatedProgress.setPlayerInternal(null);
        }
    }

    private boolean containsStorage(final @NotNull RStorage candidate) {
        return this.storages.stream().anyMatch(existingStorage -> this.matchesStorage(existingStorage, candidate));
    }

    private boolean containsStoreRequirementProgress(final @NotNull RStoreRequirementProgress candidate) {
        return this.storeRequirementProgress.stream()
            .anyMatch(existingProgress -> this.matchesStoreRequirementProgress(existingProgress, candidate));
    }

    private boolean matchesStorage(
        final @NotNull RStorage left,
        final @NotNull RStorage right
    ) {
        if (left == right) {
            return true;
        }

        final Long leftId = left.getId();
        final Long rightId = right.getId();
        return leftId != null && Objects.equals(leftId, rightId);
    }

    private boolean matchesStoreRequirementProgress(
        final @NotNull RStoreRequirementProgress left,
        final @NotNull RStoreRequirementProgress right
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

    private void removeProgressIfEmpty(final @NotNull RStoreRequirementProgress progress) {
        if (progress.isEmpty()) {
            this.removeStoreRequirementProgress(progress);
        }
    }

    private @NotNull Optional<RStoreRequirementProgress> findStoreRequirementProgress(final @NotNull String progressKey) {
        final String normalizedProgressKey = normalizeProgressKey(progressKey);
        return this.storeRequirementProgress.stream()
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
}