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

import com.raindropcentral.rdq.database.entity.rank.RRankReward;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Repository for managing persistent {@link RRankReward} entities.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class RRankRewardRepository extends CachedRepository<RRankReward, Long, Long> {
	
	/**
	 * Constructs a new {@code RRankRewardRepository} for managing {@link RRankReward} entities.
	 *
	 * @param executor             the {@link ExecutorService} used for executing repository operations asynchronously
	 * @param entityManagerFactory the {@link EntityManagerFactory} used to create and manage entity managers for persistence operations
	 * @param entityClass          the entity class type
	 * @param keyExtractor         function to extract the cache key from the entity
	 */
	public RRankRewardRepository(
		final @NotNull ExecutorService executor,
		final @NotNull EntityManagerFactory entityManagerFactory,
		@NotNull Class<RRankReward> entityClass,
		@NotNull Function<RRankReward, Long> keyExtractor
	) {
		super(
			executor,
			entityManagerFactory,
			entityClass,
			keyExtractor
		);
	}
}
