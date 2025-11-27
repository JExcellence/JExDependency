package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.perk.RPerk;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

public class RPerkRepository extends GenericCachedRepository<RPerk, Long, String> {
	
	/**
	 * Constructs a new {@code RRankRepository} for managing {@link RPerk} entities.
	 *
	 * @param executor             the {@link java.util.concurrent.ExecutorService} used for executing repository operations asynchronously
	 * @param entityManagerFactory the {@link jakarta.persistence.EntityManagerFactory} used to create and manage entity managers for persistence operations
	 */
	public RPerkRepository(
		final @NotNull ExecutorService executor,
		final @NotNull EntityManagerFactory entityManagerFactory
	) {
		
		super(
			executor,
			entityManagerFactory,
			RPerk.class,
			RPerk::getIdentifier
		);
	}
}