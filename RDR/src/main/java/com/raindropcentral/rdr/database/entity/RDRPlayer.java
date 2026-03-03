package com.raindropcentral.rdr.database.entity;

import java.util.ArrayList;
import java.util.List;
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
import org.jetbrains.annotations.NotNull;

import com.raindropcentral.rplatform.database.converter.UUIDConverter;

/**
 * Persistent player extension for RDR storage data.
 *
 * <p>Each player row owns zero or more {@link RStorage} records representing individual vaults or other
 * inventory-backed containers. The relationship cascades persistence operations so player-level updates keep
 * associated storage rows in sync.</p>
 *
 * @author RaindropCentral
 * @since 5.0.0
 * @version 5.0.0
 */
@Entity
@Table(name = "rdr_players")
public class RDRPlayer extends BaseEntity {

    @Column(name = "player_uuid", unique = true, nullable = false)
    @Convert(converter = UUIDConverter.class)
    private UUID playerUuid;

    @OneToMany(
        mappedBy = "player",
        fetch = FetchType.EAGER,
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @OrderBy("storageKey ASC")
    private List<RStorage> storages = new ArrayList<>();

    /**
     * Creates a persisted player extension for the provided UUID.
     *
     * @param playerUuid unique player identifier used as the repository key
     * @throws NullPointerException if {@code playerUuid} is {@code null}
     */
    public RDRPlayer(final @NotNull UUID playerUuid) {
        this.playerUuid = Objects.requireNonNull(playerUuid, "playerUuid cannot be null");
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

    private boolean containsStorage(final @NotNull RStorage candidate) {
        return this.storages.stream().anyMatch(existingStorage -> this.matchesStorage(existingStorage, candidate));
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
}
