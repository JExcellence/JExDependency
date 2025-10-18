/**
 * Player statistic entities representing heterogeneous metric values.
 * <p>
 * {@link com.raindropcentral.core.database.entity.statistic.RPlayerStatistic} owns a collection of
 * {@link com.raindropcentral.core.database.entity.statistic.RAbstractStatistic} derivatives and is
 * configured with {@code orphanRemoval=true}. Callers must therefore add or remove statistics via the
 * aggregate helpers—such as
 * {@link com.raindropcentral.core.database.entity.statistic.RPlayerStatistic#addOrReplaceStatistic(com.raindropcentral.core.database.entity.statistic.RAbstractStatistic)}—to
 * keep Hibernate's managed collection in sync.
 * </p>
 * <p>
 * The statistics table uses single-table inheritance with a discriminator column and a unique
 * constraint on {@code (identifier, player_statistic_id)}. Update flows should modify existing entity
 * instances in-place where possible to avoid flush ordering issues that trigger constraint
 * violations. Value serialization is delegated to the concrete subclasses (for example,
 * {@link com.raindropcentral.core.database.entity.statistic.RNumberStatistic}) which expose the typed
 * {@code getValue()} contract.
 * </p>
 */
package com.raindropcentral.core.database.entity.statistic;
