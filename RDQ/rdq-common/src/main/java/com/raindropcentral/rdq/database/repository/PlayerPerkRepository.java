package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.perk.PlayerPerk;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Repository for managing persistent {@link PlayerPerk} entities.
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class PlayerPerkRepository extends CachedRepository<PlayerPerk, Long, Long> {
	
	/**
	 * Constructs a new {@code PlayerPerkRepository} for managing {@link PlayerPerk} entities.
	 *
	 * @param executor             the {@link ExecutorService} used for executing repository operations asynchronously
	 * @param entityManagerFactory the {@link EntityManagerFactory} used to create and manage entity managers for persistence operations
	 * @param entityClass          the entity class type
	 * @param keyExtractor         function to extract the cache key from the entity
	 */
	public PlayerPerkRepository(
		final @NotNull ExecutorService executor,
		final @NotNull EntityManagerFactory entityManagerFactory,
		@NotNull Class<PlayerPerk> entityClass,
		@NotNull Function<PlayerPerk, Long> keyExtractor
	) {
		super(
			executor,
			entityManagerFactory,
			entityClass,
			keyExtractor
		);
	}
	
	/**
	 * Fetches a fresh PlayerPerk entity, applies modifications, and updates it.
	 * This method prevents OptimisticLockException by always working with the latest entity version.
	 *
	 * @param id the entity ID
	 * @param modifier the function to apply modifications to the fresh entity
	 * @return a CompletableFuture containing the updated entity, or null if not found
	 */
	public CompletableFuture<PlayerPerk> fetchAndUpdate(
		@NotNull final Long id,
		@NotNull final Consumer<PlayerPerk> modifier
	) {
		return findByIdAsync(id).thenCompose(optionalPerk -> {
			if (optionalPerk.isEmpty()) {
				return CompletableFuture.completedFuture(null);
			}
			
			PlayerPerk freshPerk = optionalPerk.get();
			modifier.accept(freshPerk);
			return updateAsync(freshPerk);
		});
	}
}
