/**
 * Rank system configuration that orchestrates tree progression and notifications.
 * <p>
 * Stage&nbsp;1 executor preparation (see {@link com.raindropcentral.rdq.RDQ}) equips the
 * configuration mapper to decode these system nodes without blocking the main thread. Stage&nbsp;2
 * {@link com.raindropcentral.rdq.RDQ#initializeComponents()} and
 * {@link com.raindropcentral.rdq.RDQ#initializeViews()} run under
 * {@link com.raindropcentral.rdq.RDQ#runSync(Runnable)}, so managers can safely read the loaded
 * system defaults while wiring UI affordances.
 * </p>
 * <p>
 * Stage&nbsp;3 assigns {@link com.raindropcentral.rdq.database.repository.RRankRepository},
 * {@link com.raindropcentral.rdq.database.repository.RPlayerRankUpgradeProgressRepository}, and
 * related repositories that consume the YAML outlined in
 * {@code rdq-common/src/main/resources/rank/README.md}. Free and premium modules share those
 * repositories; consult the RDQ manager Javadocs for notes on how edition-specific executors feed
 * progress updates back through the shared lifecycle contracts.
 * </p>
 */
package com.raindropcentral.rdq.config.ranks.system;
