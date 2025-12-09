/*
package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.perk.RPlayerPerk;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

*/
/**
 * Repository for managing persistent {@link com.raindropcentral.rdq2.database.entity.perk.RPlayerPerk} entities.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 *//*

public class RPlayerPerkRepository extends GenericCachedRepository<RPlayerPerk, Long, Long> {
	
	*/
/**
	 * Constructs a new {@code RPlayerPerkRepository} for managing {@link com.raindropcentral.rdq2.database.entity.perk.RPlayerPerk} entities.
	 *
	 * @param executor             the {@link java.util.concurrent.ExecutorService} used for asynchronous repository operations
	 * @param entityManagerFactory the {@link jakarta.persistence.EntityManagerFactory} used to create and manage entity managers
	 *//*

	public RPlayerPerkRepository(
		final @NotNull ExecutorService executor,
		final @NotNull EntityManagerFactory entityManagerFactory
	) {
		super(
			executor,
			entityManagerFactory,
			RPlayerPerk.class,
			RPlayerPerk::getId
		);
	}
}*/
