package com.raindropcentral.rds.database.repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import com.raindropcentral.rds.database.entity.ShopAdminPlayerSetting;
import de.jexcellence.hibernate.repository.BaseRepository;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Repository for persisted admin player override settings.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@SuppressWarnings({
        "unused",
        "FieldCanBeLocal"
})
/**
 * Represents the RShopAdminPlayerSetting API type.
 */
public class RShopAdminPlayerSetting extends BaseRepository<ShopAdminPlayerSetting, Long> {

    private final EntityManagerFactory entityManagerFactory;

    /**
     * Creates a new admin player settings repository.
     *
     * @param executorService executor used for repository work
     * @param entityManagerFactory entity manager factory backing the repository
     */
    public RShopAdminPlayerSetting(
            final @NotNull ExecutorService executorService,
            final @NotNull EntityManagerFactory entityManagerFactory
    ) {
        super(executorService, entityManagerFactory, ShopAdminPlayerSetting.class);
        this.entityManagerFactory = entityManagerFactory;
    }

    /**
     * Finds one player override by player UUID.
     *
     * @param playerId player UUID
     * @return matching override row, or {@code null} when none exists
     */
    public @Nullable ShopAdminPlayerSetting findByPlayerId(
            final @NotNull UUID playerId
    ) {
        final UUID normalizedPlayerId = Objects.requireNonNull(playerId, "playerId");
        return this.executeInTransaction(entityManager -> entityManager.createQuery(
                        """
                                select setting
                                from ShopAdminPlayerSetting setting
                                where setting.player_uuid = :playerId
                                """,
                        ShopAdminPlayerSetting.class
                )
                .setParameter("playerId", normalizedPlayerId)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElse(null));
    }

    /**
     * Returns all persisted player override rows sorted by player UUID text.
     *
     * @return all persisted player override rows
     */
    public @NotNull List<ShopAdminPlayerSetting> findAllEntries() {
        try (var entityManager = this.entityManagerFactory.createEntityManager()) {
            final List<ShopAdminPlayerSetting> entries = new ArrayList<>(entityManager.createQuery(
                    "SELECT entry FROM ShopAdminPlayerSetting entry",
                    ShopAdminPlayerSetting.class
            ).getResultList());
            entries.sort(Comparator.comparing(entry -> entry.getPlayerId().toString(), String.CASE_INSENSITIVE_ORDER));
            return entries;
        }
    }

    /**
     * Creates or updates one player override row.
     *
     * @param playerId player UUID
     * @param playerName optional cached player name
     * @param maximumShops optional maximum shops override
     * @param discountPercent optional discount override
     * @return persisted player override row
     */
    public @NotNull ShopAdminPlayerSetting upsert(
            final @NotNull UUID playerId,
            final @Nullable String playerName,
            final @Nullable Integer maximumShops,
            final @Nullable Double discountPercent
    ) {
        final UUID normalizedPlayerId = Objects.requireNonNull(playerId, "playerId");
        return this.executeInTransaction(entityManager -> {
            ShopAdminPlayerSetting setting = entityManager.createQuery(
                            """
                                    select existing
                                    from ShopAdminPlayerSetting existing
                                    where existing.player_uuid = :playerId
                                    """,
                            ShopAdminPlayerSetting.class
                    )
                    .setParameter("playerId", normalizedPlayerId)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .setMaxResults(1)
                    .getResultStream()
                    .findFirst()
                    .orElse(null);

            if (setting == null) {
                setting = new ShopAdminPlayerSetting(
                        normalizedPlayerId,
                        playerName,
                        maximumShops,
                        discountPercent
                );
                entityManager.persist(setting);
                return setting;
            }

            setting.setPlayerName(playerName);
            setting.setMaximumShops(maximumShops);
            setting.setDiscountPercent(discountPercent);
            return setting;
        });
    }

    /**
     * Removes one player override row when present.
     *
     * @param playerId player UUID
     */
    public void deleteByPlayerId(
            final @NotNull UUID playerId
    ) {
        final UUID normalizedPlayerId = Objects.requireNonNull(playerId, "playerId");
        this.executeInTransaction(entityManager -> {
            final ShopAdminPlayerSetting setting = entityManager.createQuery(
                            """
                                    select existing
                                    from ShopAdminPlayerSetting existing
                                    where existing.player_uuid = :playerId
                                    """,
                            ShopAdminPlayerSetting.class
                    )
                    .setParameter("playerId", normalizedPlayerId)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .setMaxResults(1)
                    .getResultStream()
                    .findFirst()
                    .orElse(null);

            if (setting != null) {
                entityManager.remove(setting);
            }
            return null;
        });
    }
}
