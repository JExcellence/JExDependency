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
	 * Constructs a new {@code RPlayerRankRepository} for managing {@link RPlayerRank} entities.
	 *
	 * @param executor             the {@link ExecutorService} used for asynchronous repository operations
	 * @param entityManagerFactory the {@link EntityManagerFactory} used to create and manage entity managers
	 */
	private final EntityManagerFactory entityManagerFactory;
	private final ExecutorService executor;

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