package com.raindropcentral.rds.database.repository;

import com.raindropcentral.rds.database.entity.Shop;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

@SuppressWarnings({
        "unused",
        "FieldCanBeLocal"
})
public class RShop extends CachedRepository<Shop, Long, Location> {

    private final EntityManagerFactory emf;

    public RShop(
            @NotNull ExecutorService executorService,
            @NotNull EntityManagerFactory entityManagerFactory,
            @NotNull Class<Shop> entityClass,
            @NotNull Function<Shop, Location> keyExtractor
    ) {
        super(executorService, entityManagerFactory, entityClass, keyExtractor);
        this.emf = entityManagerFactory;
    }

    public Shop findByLocation(Location location) {
        return findByAttributes(Map.of("shop_location", location)).orElse(null);
    }
}
