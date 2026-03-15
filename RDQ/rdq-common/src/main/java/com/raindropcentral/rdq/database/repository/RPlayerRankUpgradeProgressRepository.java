package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.rank.RPlayerRankUpgradeProgress;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 *
  * Documents this API member.
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class RPlayerRankUpgradeProgressRepository extends CachedRepository<RPlayerRankUpgradeProgress, Long, Long> {
	
	/**
	 *
	  * Documents this API member.
	 * @param executor             the {@link java.util.concurrent.ExecutorService} used for asynchronous repository operations
	 * @param entityManagerFactory the {@link jakarta.persistence.EntityManagerFactory} used to create and manage entity managers
	 */
	public RPlayerRankUpgradeProgressRepository(
		final @NotNull ExecutorService executor,
		final @NotNull EntityManagerFactory entityManagerFactory,
		@NotNull Class<RPlayerRankUpgradeProgress> entityClass,
		@NotNull Function<RPlayerRankUpgradeProgress, Long> keyExtractor
	) {
		super(
			executor,
			entityManagerFactory,
			entityClass,
			keyExtractor
		);
	}
}
