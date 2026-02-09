package com.raindropcentral.rds.database.repository;

import com.raindropcentral.rds.database.entity.RDSPlayer;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

@SuppressWarnings({
        "unused",
        "FieldCanBeLocal"
})
public class RRDSPlayer extends CachedRepository<RDSPlayer, Long, UUID> {

    private final EntityManagerFactory emf;

    public RRDSPlayer(
            @NotNull ExecutorService executorService,
            @NotNull EntityManagerFactory entityManagerFactory,
            @NotNull Class<RDSPlayer> entityClass,
            @NotNull Function<RDSPlayer, UUID> keyExtractor
    ) {
        super(executorService, entityManagerFactory, entityClass, keyExtractor);
        this.emf = entityManagerFactory;
    }

    public RDSPlayer findByPlayer(UUID player_uuid) {
        return findByAttributes(Map.of("player_uuid", player_uuid)).orElse(null);
    }

}