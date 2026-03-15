package com.raindropcentral.rds.database.repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

import com.raindropcentral.rds.database.entity.ShopAdminGroupSetting;
import de.jexcellence.hibernate.repository.BaseRepository;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Repository for persisted admin group override settings.
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
 * Represents the RShopAdminGroupSetting API type.
 */
public class RShopAdminGroupSetting extends BaseRepository<ShopAdminGroupSetting, Long> {

    private final EntityManagerFactory entityManagerFactory;

    /**
     * Creates a new admin group settings repository.
     *
     * @param executorService executor used for repository work
     * @param entityManagerFactory entity manager factory backing the repository
     */
    public RShopAdminGroupSetting(
            final @NotNull ExecutorService executorService,
            final @NotNull EntityManagerFactory entityManagerFactory
    ) {
        super(executorService, entityManagerFactory, ShopAdminGroupSetting.class);
        this.entityManagerFactory = entityManagerFactory;
    }

    /**
     * Finds one group override by group name.
     *
     * @param groupName group identifier
     * @return matching override row, or {@code null} when none exists
     */
    public @Nullable ShopAdminGroupSetting findByGroupName(
            final @NotNull String groupName
    ) {
        final String normalized = normalizeGroupName(groupName);
        return this.executeInTransaction(entityManager -> entityManager.createQuery(
                        """
                                select setting
                                from ShopAdminGroupSetting setting
                                where setting.group_name = :groupName
                                """,
                        ShopAdminGroupSetting.class
                )
                .setParameter("groupName", normalized)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElse(null));
    }

    /**
     * Returns all persisted group override rows sorted by group name.
     *
     * @return all persisted group override rows
     */
    public @NotNull List<ShopAdminGroupSetting> findAllEntries() {
        try (var entityManager = this.entityManagerFactory.createEntityManager()) {
            final List<ShopAdminGroupSetting> entries = new ArrayList<>(entityManager.createQuery(
                    "SELECT entry FROM ShopAdminGroupSetting entry",
                    ShopAdminGroupSetting.class
            ).getResultList());
            entries.sort(Comparator.comparing(ShopAdminGroupSetting::getGroupName, String.CASE_INSENSITIVE_ORDER));
            return entries;
        }
    }

    /**
     * Creates or updates one group override row.
     *
     * @param groupName group identifier
     * @param maximumShops optional maximum shops override
     * @param discountPercent optional discount override
     * @return persisted group override row
     */
    public @NotNull ShopAdminGroupSetting upsert(
            final @NotNull String groupName,
            final @Nullable Integer maximumShops,
            final @Nullable Double discountPercent
    ) {
        final String normalizedGroupName = normalizeGroupName(groupName);
        return this.executeInTransaction(entityManager -> {
            ShopAdminGroupSetting setting = entityManager.createQuery(
                            """
                                    select existing
                                    from ShopAdminGroupSetting existing
                                    where existing.group_name = :groupName
                                    """,
                            ShopAdminGroupSetting.class
                    )
                    .setParameter("groupName", normalizedGroupName)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .setMaxResults(1)
                    .getResultStream()
                    .findFirst()
                    .orElse(null);

            if (setting == null) {
                setting = new ShopAdminGroupSetting(
                        normalizedGroupName,
                        maximumShops,
                        discountPercent
                );
                entityManager.persist(setting);
                return setting;
            }

            setting.setMaximumShops(maximumShops);
            setting.setDiscountPercent(discountPercent);
            return setting;
        });
    }

    /**
     * Removes one group override row when present.
     *
     * @param groupName group identifier
     */
    public void deleteByGroupName(
            final @NotNull String groupName
    ) {
        final String normalizedGroupName = normalizeGroupName(groupName);
        this.executeInTransaction(entityManager -> {
            final ShopAdminGroupSetting setting = entityManager.createQuery(
                            """
                                    select existing
                                    from ShopAdminGroupSetting existing
                                    where existing.group_name = :groupName
                                    """,
                            ShopAdminGroupSetting.class
                    )
                    .setParameter("groupName", normalizedGroupName)
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

    private static @NotNull String normalizeGroupName(
            final @NotNull String groupName
    ) {
        final String normalized = Objects.requireNonNull(groupName, "groupName")
                .trim()
                .toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Group name cannot be blank");
        }
        return normalized;
    }
}
