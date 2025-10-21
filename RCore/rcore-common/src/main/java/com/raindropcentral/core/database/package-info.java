/**
 * Provides the aggregate root definitions and repository contracts for the core database module.
 * <p>
 * Aggregates are assembled prior to persistence to ensure consistent writes. For example, a
 * {@link com.raindropcentral.core.database.entity.player.RPlayer} must have its
 * {@link com.raindropcentral.core.database.entity.statistic.RPlayerStatistic} and nested statistics
 * composed in-memory before repositories attempt a flush. This prevents partial insertions that can
 * violate the single-table inheritance constraints defined by
 * {@link com.raindropcentral.core.database.entity.statistic.RAbstractStatistic} and its subclasses.
 * </p>
 * <p>
 * The module relies on {@link com.raindropcentral.rplatform.logging.CentralLogger} to surface
 * aggregate lifecycle milestones. Entities should log important events—such as the creative-mode
 * inventory bypass recorded by
 * {@link com.raindropcentral.core.database.entity.inventory.RPlayerInventory}—so consumers can
 * correlate persistence decisions across modules.
 * </p>
 * <p>
 * Repository implementations extend {@code GenericCachedRepository} to provide asynchronous, cached
 * data access. Each repository is responsible for:
 * </p>
 * <ul>
 *     <li>Maintaining identifier-based caches keyed by aggregate natural IDs (for example, player and
 *     server UUIDs in
 *     {@link com.raindropcentral.core.database.repository.RPlayerRepository} and
 *     {@link com.raindropcentral.core.database.repository.RServerRepository}).</li>
 *     <li>Enforcing create-or-update semantics around aggregate roots so that callers can treat
 *     writes as idempotent operations.</li>
 *     <li>Delegating executor usage to the lifecycle-managed thread pools supplied during
 *     construction, keeping blocking calls off the main thread.</li>
 * </ul>
 */
package com.raindropcentral.core.database;
