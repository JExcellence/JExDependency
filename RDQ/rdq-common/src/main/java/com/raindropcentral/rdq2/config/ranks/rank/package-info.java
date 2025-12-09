/**
 * Leaf rank configuration nodes used during RDQ's enablement stages.
 * <p>
 * Stage&nbsp;1 of the enable pipeline initialises the shared executor described in
 * {@link com.raindropcentral.rdq2.RDQ}, ensuring configuration parsing can delegate to virtual
 * threads and fall back to the fixed pool if the runtime lacks virtual thread support. When
 * {@link com.raindropcentral.rdq2.RDQ#initializeComponents()} and
 * {@link com.raindropcentral.rdq2.RDQ#initializeViews()} execute within the
 * {@link com.raindropcentral.rdq2.RDQ#runSync(Runnable) runSync} boundary (stage&nbsp;2), rank
 * definitions remain immutable so GUI binding and command registration can safely reference them.
 * </p>
 * <p>
 * Repository wiring (stage&nbsp;3) feeds the descriptors into
 * {@link com.raindropcentral.rdq2.database.repository.RRankRepository} and
 * {@link com.raindropcentral.rdq2.database.repository.RPlayerRankRepository}. Those repositories
 * hydrate YAML documented in {@code rdq-common/src/main/resources/rank/} before free and premium
 * managers coordinate promotions, keeping behaviour in sync with the expectations captured in the
 * edition Javadocs.
 * </p>
 */
package com.raindropcentral.rdq2.config.ranks.rank;
