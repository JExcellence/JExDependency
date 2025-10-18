/**
 * Bounty configuration sections.
 * <p>
 * {@link com.raindropcentral.rdq.config.bounty.BountySection} captures the settings that drive
 * {@link com.raindropcentral.rdq.type.EBountyClaimMode} evaluation at runtime. Values are sourced
 * from {@code rdq-common/src/main/resources/bounty/bounty.yml} and are consumed by
 * gameplay services to choose the appropriate claim resolution strategy.
 * </p>
 * <p>
 * After altering bounty configuration keys, keep {@code bounty.yml} in sync and execute
 * {@code ./gradlew :RDQ:rdq-common:check} so mapper validation catches mismatches early.
 * </p>
 */
package com.raindropcentral.rdq.config.bounty;
