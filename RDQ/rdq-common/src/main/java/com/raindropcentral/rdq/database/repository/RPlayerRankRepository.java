package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.rank.RPlayerRank;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Repository for managing persistent {@link com.raindropcentral.rdq.database.entity.rank.RPlayerRank} entities.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class RPlayerRankRepository extends GenericCachedRepository<RPlayerRank, Long, Long> {
	
	/**
	 * Constructs a new {@code RPlayerRankRepository} for managing {@link RPlayerRank} entities.
	 *
	 * @param executor             the {@link ExecutorService} used for asynchronous repository operations
	 * @param entityManagerFactory the {@link EntityManagerFactory} used to create and manage entity managers
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
	}
}