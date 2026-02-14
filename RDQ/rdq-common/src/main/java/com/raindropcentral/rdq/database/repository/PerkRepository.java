package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.perk.Perk;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Repository for managing persistent {@link Perk} entities.
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class PerkRepository extends CachedRepository<Perk, Long, String> {
	
	/**
	 * Constructs a new {@code PerkRepository} for managing {@link Perk} entities.
	 *
	 * @param executor             the {@link ExecutorService} used for executing repository operations asynchronously
	 * @param entityManagerFactory the {@link EntityManagerFactory} used to create and manage entity managers for persistence operations
	 * @param entityClass          the entity class type
	 * @param keyExtractor         function to extract the cache key from the entity
	 */
	public PerkRepository(
		final @NotNull ExecutorService executor,
		final @NotNull EntityManagerFactory entityManagerFactory,
		@NotNull Class<Perk> entityClass,
		@NotNull Function<Perk, String> keyExtractor
	) {
		super(
			executor,
			entityManagerFactory,
			entityClass,
			keyExtractor
		);
	}
}
