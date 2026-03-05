package com.raindropcentral.rds.database.repository;

import com.raindropcentral.rds.database.entity.Shop;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Represents r shop.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@SuppressWarnings({
        "unused",
        "FieldCanBeLocal"
})
public class RShop extends CachedRepository<Shop, Long, Location> {

    private static final Logger LOGGER = LoggerFactory.getLogger("RDS");

    private final EntityManagerFactory emf;

    /**
     * Creates a new r shop.
     *
     * @param executorService executor used for repository work
     * @param entityManagerFactory entity manager factory backing the repository
     * @param entityClass entity type managed by the repository
     * @param keyExtractor cache key extractor for loaded entities
     */
    public RShop(
            @NotNull ExecutorService executorService,
            @NotNull EntityManagerFactory entityManagerFactory,
            @NotNull Class<Shop> entityClass,
            @NotNull Function<Shop, Location> keyExtractor
    ) {
        super(executorService, entityManagerFactory, entityClass, keyExtractor);
        this.emf = entityManagerFactory;
    }

    /**
     * Resolves a shop by either chest block location.
     *
     * @param location block location to search for
     * @return the matching shop, or {@code null} when no shop occupies that location
     */
    public Shop findByLocation(
            final Location location
    ) {
        if (location == null) {
            return null;
        }

        final Shop primaryShop = findByAttributes(Map.of("shop_location", location)).orElse(null);
        if (primaryShop != null) {
            return primaryShop;
        }

        return findByAttributes(Map.of("secondary_shop_location", location)).orElse(null);
    }

    /**
     * Finds all shops.
     *
     * @return the matched all shops, or {@code null} when none exists
     */
    public @NotNull List<Shop> findAllShops() {
        try (var entityManager = this.emf.createEntityManager()) {
            final List<?> shopIds = entityManager.createNativeQuery(
                    "SELECT id FROM shops ORDER BY id"
            ).getResultList();

            final List<Shop> shops = new ArrayList<>(shopIds.size());
            for (final Object shopId : shopIds) {
                if (!(shopId instanceof Number numericId)) {
                    continue;
                }

                final Shop shop = this.findShopByIdSafely(numericId.longValue());
                if (shop != null) {
                    shops.add(shop);
                }
            }

            return shops;
        }
    }

    private Shop findShopByIdSafely(
            final long shopId
    ) {
        try (var entityManager = this.emf.createEntityManager()) {
            return entityManager.find(Shop.class, shopId);
        } catch (PersistenceException | IllegalArgumentException exception) {
            final String rootCauseMessage = this.getRootCauseMessage(exception);
            LOGGER.warn(
                    "Skipping shop {} while loading all shops. The persisted shop record is invalid, likely due to an unresolved world in shop_location. Cause: {}",
                    shopId,
                    rootCauseMessage
            );
            return null;
        }
    }

    private @NotNull String getRootCauseMessage(
            final @NotNull Throwable throwable
    ) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }

        final String message = current.getMessage();
        return message == null || message.isBlank()
                ? current.getClass().getSimpleName()
                : message;
    }
}
