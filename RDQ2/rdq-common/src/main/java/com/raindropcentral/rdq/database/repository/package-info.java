/**
 * Hibernate-backed repositories that provide asynchronous access to RDQ entities.
 * <p>
 * Every repository extends {@link de.jexcellence.hibernate.repository.GenericCachedRepository} to
 * expose CRUD operations backed by the {@link jakarta.persistence.EntityManagerFactory} supplied during
 * the enable sequence outlined in {@link com.raindropcentral.rdq.RDQ#onEnable()}, ensuring they share
 * the executor used throughout the pipeline. The repositories encapsulate query utilities for each gameplay
 * aggregate—for example {@link com.raindropcentral.rdq.database.repository.RDQPlayerRepository}
 * manages {@link com.raindropcentral.rdq.database.entity.player.RDQPlayer} records while
 * {@link com.raindropcentral.rdq.database.repository.RPlayerQuestRepository} handles quest progress
 * tracking.
 * </p>
 *
 * <p>
 * Instances are created during the final <em>repository wiring</em> phase after platform bootstrap and
 * view registration, so calling code can safely perform persistence operations once the
 * {@link com.raindropcentral.rdq.RDQ#onEnable()} lifecycle has progressed to repository wiring.
 * </p>
 */
package com.raindropcentral.rdq.database.repository;
