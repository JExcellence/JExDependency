/**
 * Concrete perk section nodes participating in RDQ's staged enable pipeline.
 * <p>
 * During the asynchronous platform bootstrap (stage&nbsp;1) {@link com.raindropcentral.rdq.RDQ}
 * creates the shared executor that prefers virtual threads and falls back to a fixed pool when
 * necessary. The platform stage prepares the configuration mapper so that, once
 * {@link com.raindropcentral.rdq.RDQ#initializeComponents()} and
 * {@link com.raindropcentral.rdq.RDQ#initializeViews()} finish on the main thread (stage&nbsp;2),
 * each section can be safely materialised inside the {@link com.raindropcentral.rdq.RDQ#runSync(Runnable)
 * runSync} boundary without blocking server ticks.
 * </p>
 * <p>
 * Repository wiring (stage&nbsp;3) invokes {@link com.raindropcentral.rdq.database.repository.RPerkRepository}
 * to hydrate the YAML descriptors documented in {@code rdq-common/src/main/resources/perks/README.md}.
 * Implementations consume these section classes immediately before the repository exposes
 * immutable perk definitions to edition-specific services such as the premium perk manager.
 * </p>
 * <p>
 * Contributors should review the free and premium bounty manager Javadocs for cross-edition
 * expectations about repository usage to keep configuration and runtime orchestration aligned.
 * </p>
 */
package com.raindropcentral.rdq.config.perk.sections;
