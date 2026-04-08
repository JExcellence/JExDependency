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
 * Repository for managing persistent {@link RPlayerRank} entities.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class RPlayerRankRepository extends CachedRepository<RPlayerRank, Long, Long> {
	
	/**
	 * Entity manager factory used to create repository sessions.
	 */
	private final EntityManagerFactory entityManagerFactory;

	/**
	 * Executor used for asynchronous repository operations.
	 */
	private final ExecutorService executor;

	/**
	 * Constructs a new {@code RPlayerRankRepository} for managing {@link RPlayerRank} entities.
	 *
	 * @param executor             the {@link ExecutorService} used for asynchronous repository operations
	 * @param entityManagerFactory the {@link EntityManagerFactory} used to create and manage entity managers
	 * @param entityClass          the managed entity type
	 * @param keyExtractor         the cache key extractor
	 */
	public RPlayerRankRepository(
		final @NotNull ExecutorService executor,
		final @NotNull EntityManagerFactory entityManagerFactory,
		@NotNull Class<RPlayerRank> entityClass,
		@NotNull Function<RPlayerRank, Long> keyExtractor
	) {
		super(executor, entityManagerFactory, entityClass, keyExtractor);
		this.entityManagerFactory = entityManagerFactory;
		this.executor = executor;
	}

	/**
	 * Returns whether a player has an entry for the supplied rank identifier.
	 *
	 * @param playerId the player identifier to inspect
	 * @param rankId the rank identifier to look up
	 * @return a future completing with {@code true} when the player has the rank
	 */
	public CompletableFuture<Boolean> existsByPlayerIdAndRankId(@NotNull final UUID playerId, final Long rankId) {
		return CompletableFuture.supplyAsync(() -> {
			EntityManager em = entityManagerFactory.createEntityManager();
			try {
				Long count = em.createQuery(
					"SELECT COUNT(r) FROM RPlayerRank r WHERE r.player.uniqueId = :playerId AND r.currentRank.id = :rankId",
					Long.class
				).setParameter("playerId", playerId).setParameter("rankId", rankId).getSingleResult();
				return count > 0;
			} finally { em.close(); }
		}, executor);
	}

	/**
	 * Finds a player's rank entry for the supplied rank identifier.
	 *
	 * @param playerId the player identifier to inspect
	 * @param rankId the rank identifier to look up
	 * @return a future completing with the matching rank entry when present
	 */
	public CompletableFuture<Optional<RPlayerRank>> findByPlayerIdAndRankId(@NotNull final UUID playerId, final Long rankId) {
		return CompletableFuture.supplyAsync(() -> {
			EntityManager em = entityManagerFactory.createEntityManager();
			try {
				var results = em.createQuery(
					"SELECT r FROM RPlayerRank r WHERE r.player.uniqueId = :playerId AND r.currentRank.id = :rankId",
					RPlayerRank.class
				).setParameter("playerId", playerId).setParameter("rankId", rankId).getResultList();
				return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
			} finally { em.close(); }
		}, executor);
	}

	/**
	 * Finds the player's rank entry matching the requested active state.
	 *
	 * @param playerId the player identifier to inspect
	 * @param active whether the returned rank entry must be active
	 * @return a future completing with the matching rank entry when present
	 */
	public CompletableFuture<Optional<RPlayerRank>> findByPlayerIdAndIsActive(@NotNull final UUID playerId, final boolean active) {
		return CompletableFuture.supplyAsync(() -> {
			EntityManager em = entityManagerFactory.createEntityManager();
			try {
				var results = em.createQuery(
					"SELECT r FROM RPlayerRank r WHERE r.player.uniqueId = :playerId AND r.isActive = :active",
					RPlayerRank.class
				).setParameter("playerId", playerId).setParameter("active", active).getResultList();
				return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
			} finally { em.close(); }
		}, executor);
	}

	/**
	 * Loads every rank entry associated with the supplied player.
	 *
	 * @param playerId the player identifier to inspect
	 * @return a future completing with every rank entry owned by the player
	 */
	public CompletableFuture<List<RPlayerRank>> findByPlayerId(@NotNull final UUID playerId) {
		return CompletableFuture.supplyAsync(() -> {
			EntityManager em = entityManagerFactory.createEntityManager();
			try {
				return em.createQuery(
					"SELECT r FROM RPlayerRank r WHERE r.player.uniqueId = :playerId",
					RPlayerRank.class
				).setParameter("playerId", playerId).getResultList();
			} finally { em.close(); }
		}, executor);
	}
}
