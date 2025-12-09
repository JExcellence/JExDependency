/**
 * Rank system bootstrapping utilities.
 * <p>
 * Classes in this package ({@link com.raindropcentral.rdq2.utility.rank.RankSystemFactory},
 * {@link com.raindropcentral.rdq2.utility.rank.RankValidationService}, and
 * {@link com.raindropcentral.rdq2.utility.rank.RankEntityService}) cooperate to load configuration,
 * validate prerequisite graphs, persist entities, and surface an immutable
 * {@link com.raindropcentral.rdq2.utility.rank.RankSystemState}. The staged asynchronous pipeline
 * mirrors the manager lifecycle so views and services receive a fully validated snapshot regardless
 * of edition.
 * </p>
 */
package com.raindropcentral.rdq2.utility.rank;
