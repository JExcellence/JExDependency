/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.rank.RPlayerRank;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Repository for managing persistent {@link com.raindropcentral.rdq.database.entity.rank.RPlayerRank} entities.
 * <p>
 * This repository provides data access methods for player rank tracking,
 * supporting the RPlatform progression system integration.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class RPlayerRankRepository extends CachedRepository<RPlayerRank, Long, Long> {
	
	private final EntityManagerFactory entityManagerFactory;
	private final ExecutorService executor;
	
	/**
	 * Constructs a new {@code RPlayerRankRepository} for managing {@link RPlayerRank} entities.
	 *
	 * @param executor             the {@link ExecutorService} used for asynchronous repository operations
	 * @param entityManagerFactory the {@link EntityManagerFactory} used to create and manage entity managers
	 * @param entityClass          the entity class
	 * @param keyExtractor         function to extract the key from an entity
	 */
	public RPlayerRankRepository(
		final @NotNull ExecutorService executor,
		final @NotNull EntityManagerFactory entityManagerFactory,
		@NotNull Class<RPlayerRank> entityClass,
		@NotNull Function<RPlayerRank, Long> keyExtractor
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
	 * Finds a player's rank record by player UUID and rank ID.
	 * <p>
	 * This method is used by the progression system to check if a player
	 * has achieved a specific rank.
	 * </p>
	 *
	 * @param playerId the player UUID
	 * @param rankId the rank ID
	 * @return CompletableFuture containing the player rank record if found
	 */
	@NotNull
	public CompletableFuture<Optional<RPlayerRank>> findByPlayerIdAndRankId(
		@NotNull UUID playerId,
		@NotNull Long rankId
	) {
		return CompletableFuture.supplyAsync(() -> {
			EntityManager em = entityManagerFactory.createEntityManager();
			try {
				List<RPlayerRank> results = em.createQuery(
					"SELECT pr FROM RPlayerRank pr " +
					"WHERE pr.player.playerId = :playerId " +
					"AND pr.currentRank.id = :rankId",
					RPlayerRank.class
				)
				.setParameter("playerId", playerId)
				.setParameter("rankId", rankId)
				.getResultList();
				
				return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
			} finally {
				em.close();
			}
		}, executor);
	}
	
	/**
	 * Finds all active rank records for a player.
	 * <p>
	 * This returns all ranks the player currently holds across all rank trees.
	 * Used by the progression system to determine completed ranks.
	 * </p>
	 *
	 * @param playerId the player UUID
	 * @param isActive whether to find active or inactive ranks
	 * @return CompletableFuture containing list of player rank records
	 */
	@NotNull
	public CompletableFuture<List<RPlayerRank>> findByPlayerIdAndIsActive(
		@NotNull UUID playerId,
		boolean isActive
	) {
		return CompletableFuture.supplyAsync(() -> {
			EntityManager em = entityManagerFactory.createEntityManager();
			try {
				return em.createQuery(
					"SELECT pr FROM RPlayerRank pr " +
					"WHERE pr.player.playerId = :playerId " +
					"AND pr.isActive = :isActive",
					RPlayerRank.class
				)
				.setParameter("playerId", playerId)
				.setParameter("isActive", isActive)
				.getResultList();
			} finally {
				em.close();
			}
		}, executor);
	}
	
	/**
	 * Finds all rank records for a player (active and inactive).
	 * <p>
	 * This returns the player's rank history across all rank trees.
	 * </p>
	 *
	 * @param playerId the player UUID
	 * @return CompletableFuture containing list of all player rank records
	 */
	@NotNull
	public CompletableFuture<List<RPlayerRank>> findByPlayerId(@NotNull UUID playerId) {
		return CompletableFuture.supplyAsync(() -> {
			EntityManager em = entityManagerFactory.createEntityManager();
			try {
				return em.createQuery(
					"SELECT pr FROM RPlayerRank pr " +
					"WHERE pr.player.playerId = :playerId",
					RPlayerRank.class
				)
				.setParameter("playerId", playerId)
				.getResultList();
			} finally {
				em.close();
			}
		}, executor);
	}
	
	/**
	 * Checks if a player has achieved a specific rank.
	 * <p>
	 * This is a convenience method for the progression system to quickly
	 * check rank completion status.
	 * </p>
	 *
	 * @param playerId the player UUID
	 * @param rankId the rank ID
	 * @return CompletableFuture containing true if player has the rank, false otherwise
	 */
	@NotNull
	public CompletableFuture<Boolean> existsByPlayerIdAndRankId(
		@NotNull UUID playerId,
		@NotNull Long rankId
	) {
		return findByPlayerIdAndRankId(playerId, rankId)
			.thenApply(Optional::isPresent);
	}
}
