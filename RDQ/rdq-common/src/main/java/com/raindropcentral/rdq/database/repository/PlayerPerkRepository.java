package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.perk.PlayerPerk;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;
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
	
	private final EntityManagerFactory entityManagerFactory;
	private final ExecutorService executor;
	
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
		this.entityManagerFactory = entityManagerFactory;
		this.executor = executor;
	}
	
	/**
	 * Finds all PlayerPerks for a specific player with eagerly loaded Perk data.
	 * This prevents LazyInitializationException when accessing perks from cache.
	 * 
	 * @param playerId the player's UUID
	 * @return a CompletableFuture containing the list of PlayerPerks with initialized Perks
	 */
	public CompletableFuture<List<PlayerPerk>> findAllByPlayerIdWithPerk(@NotNull final UUID playerId) {
		return CompletableFuture.supplyAsync(() -> {
			var em = this.entityManagerFactory.createEntityManager();
			try {
				// Use JOIN FETCH to eagerly load the Perk relationship
				// This prevents LazyInitializationException when accessing perks outside the session
				return em.createQuery(
					"SELECT pp FROM PlayerPerk pp " +
					"JOIN FETCH pp.perk " +
					"WHERE pp.player.uniqueId = :playerId",
					PlayerPerk.class
				)
				.setParameter("playerId", playerId)
				.getResultList();
			} finally {
				if (em.isOpen()) {
					em.close();
				}
			}
		}, this.executor);
	}
	
	/**
	 * Fetches a fresh PlayerPerk entity, applies modifications, and updates it.
	 * This method prevents OptimisticLockException by always working with the latest entity version.
	 * 
	 * <p><b>UPDATED:</b> Now uses updatePlayerPerkOnly() to avoid cascade to Perk entity.</p>
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
			
			// CRITICAL: Use updatePlayerPerkOnly instead of updateAsync
			// This prevents cascade to the Perk entity
			return updatePlayerPerkOnly(freshPerk);
		});
	}
	
	/**
	 * Updates only PlayerPerk fields without cascading to the Perk entity.
	 * This prevents OptimisticLockException on the Perk entity when multiple
	 * players have the same perk active simultaneously.
	 * 
	 * <p><b>CRITICAL:</b> This method uses JPQL UPDATE to modify only PlayerPerk
	 * fields, completely avoiding any cascade operations to the Perk entity.</p>
	 *
	 * @param playerPerk the PlayerPerk entity to update
	 * @return a CompletableFuture containing the updated entity
	 */
	public CompletableFuture<PlayerPerk> updatePlayerPerkOnly(@NotNull final PlayerPerk playerPerk) {
		return CompletableFuture.supplyAsync(() -> {
			var em = this.entityManagerFactory.createEntityManager();
			try {
				em.getTransaction().begin();
				
				// Use JPQL update to only modify PlayerPerk fields
				// This completely avoids loading or updating the Perk entity
				int updated = em.createQuery(
					"UPDATE PlayerPerk pp SET " +
					"pp.unlocked = :unlocked, " +
					"pp.enabled = :enabled, " +
					"pp.active = :active, " +
					"pp.cooldownExpiresAt = :cooldown, " +
					"pp.activationCount = :count, " +
					"pp.totalUsageTimeMillis = :usage, " +
					"pp.lastActivated = :lastActivated, " +
					"pp.lastDeactivated = :lastDeactivated, " +
					"pp.updatedAt = CURRENT_TIMESTAMP, " +
					"pp.version = pp.version + 1 " +
					"WHERE pp.id = :id AND pp.version = :version"
				)
				.setParameter("unlocked", playerPerk.isUnlocked())
				.setParameter("enabled", playerPerk.isEnabled())
				.setParameter("active", playerPerk.isActive())
				.setParameter("cooldown", playerPerk.getCooldownExpiresAt())
				.setParameter("count", playerPerk.getActivationCount())
				.setParameter("usage", playerPerk.getTotalUsageTimeMillis())
				.setParameter("lastActivated", playerPerk.getLastActivated())
				.setParameter("lastDeactivated", playerPerk.getLastDeactivated())
				.setParameter("id", playerPerk.getId())
				.setParameter("version", playerPerk.getVersion())
				.executeUpdate();
				
				em.getTransaction().commit();
				
				if (updated == 0) {
					// Version mismatch - entity was modified elsewhere
					throw new jakarta.persistence.OptimisticLockException(
						"PlayerPerk was modified by another transaction"
					);
				}
				
				// Increment version locally to keep entity in sync
				playerPerk.setVersion(playerPerk.getVersion() + 1);
				
				return playerPerk;
			} catch (Exception e) {
				if (em.getTransaction().isActive()) {
					try {
						em.getTransaction().rollback();
					} catch (Exception rollbackEx) {
						// Log but don't throw - connection may be closed
						java.util.logging.Logger.getLogger(PlayerPerkRepository.class.getName())
							.fine("Rollback failed (connection may be closed): " + rollbackEx.getMessage());
					}
				}
				throw e;
			} finally {
				if (em.isOpen()) {
					em.close();
				}
			}
		}, this.executor);
	}
}
