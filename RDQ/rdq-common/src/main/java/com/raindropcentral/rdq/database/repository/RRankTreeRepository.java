package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.rank.RRankTree;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Repository for managing persistent {@link com.raindropcentral.rdq.database.entity.rank.RRankTree} entities.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class RRankTreeRepository extends GenericCachedRepository<RRankTree, Long, String> {
	
	/**
	 * Constructs a new {@code RRankTreeRepository} for managing {@link RRankTree} entities.
	 *
	 * @param executor             the {@link ExecutorService} used for executing repository operations asynchronously
	 * @param entityManagerFactory the {@link EntityManagerFactory} used to create and manage entity managers for persistence operations
	 */
	public RRankTreeRepository(
		final @NotNull ExecutorService executor,
		final @NotNull EntityManagerFactory entityManagerFactory,
		@NotNull Class<RRankTree> entityClass,
		@NotNull Function<RRankTree, String> keyExtractor
	) {
		
		super(
			executor,
			entityManagerFactory,
			entityClass, keyExtractor
		);
	}
}