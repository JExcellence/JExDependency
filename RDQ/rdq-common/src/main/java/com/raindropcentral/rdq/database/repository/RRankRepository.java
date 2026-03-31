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

import com.raindropcentral.rdq.database.entity.rank.RRank;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Repository for managing persistent {@link RRank} entities.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class RRankRepository extends CachedRepository<RRank, Long, String> {

	private final EntityManagerFactory entityManagerFactory;
	private final ExecutorService executor;

	/**
	 * Constructs a new {@code RRankRepository} for managing {@link RRank} entities.
	 *
	 * @param executor             the {@link ExecutorService} used for executing repository operations asynchronously
	 * @param entityManagerFactory the {@link EntityManagerFactory} used to create and manage entity managers for persistence operations
	 */
	public RRankRepository(
		final @NotNull ExecutorService executor,
		final @NotNull EntityManagerFactory entityManagerFactory,
		@NotNull Class<RRank> entityClass,
		@NotNull Function<RRank, String> keyExtractor
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
	 * Finds a rank by its unique identifier.
	 *
	 * @param identifier the rank identifier
	 * @return a CompletableFuture containing the optional rank
	 */
	@NotNull
	public CompletableFuture<Optional<RRank>> findByIdentifier(@NotNull final String identifier) {
		return CompletableFuture.supplyAsync(() -> {
			EntityManager em = entityManagerFactory.createEntityManager();
			try {
				var results = em.createQuery(
					"SELECT r FROM RRank r WHERE r.identifier = :identifier",
					RRank.class
				)
				.setParameter("identifier", identifier)
				.getResultList();

				return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
			} finally {
				em.close();
			}
		}, executor);
	}
}