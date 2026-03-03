package com.raindropcentral.rdr.database.repository;

import com.raindropcentral.rdr.database.entity.RDRPlayer;
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
public class RRDRPlayer extends CachedRepository<RDRPlayer, Long, UUID> {
	
	private final EntityManagerFactory emf;
	
	public RRDRPlayer(
		@NotNull ExecutorService executorService,
		@NotNull EntityManagerFactory entityManagerFactory,
		@NotNull Class<RDRPlayer> entityClass,
		@NotNull Function<RDRPlayer, UUID> keyExtractor
	) {
		super(executorService, entityManagerFactory, entityClass, keyExtractor);
		this.emf = entityManagerFactory;
	}
	
	public RDRPlayer findByPlayer(UUID player_uuid) {
		return findByAttributes(Map.of("player_uuid", player_uuid)).orElse(null);
	}
	
}