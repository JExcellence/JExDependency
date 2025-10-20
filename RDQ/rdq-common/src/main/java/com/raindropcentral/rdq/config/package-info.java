/**
 * Core configuration sections for the RaindropQuests module.
 * <p>
 * Classes in this package model the top-level YAML documents that live under
 * {@code rdq-common/src/main/resources}, using the JEConfig {@link de.jexcellence.configmapper.sections.AConfigSection}
 * hierarchy to provide strongly-typed access to quest, rank, perk, and bounty configuration.
 * These sections are shared across database converters, factories, and runtime assemblers.
 * </p>
 * <p>
 * Whenever a configuration section is extended, update the matching YAML resource
 * (for example {@code bounty/bounty.yml}, {@code perks/*.yml}, or {@code rank/paths/*.yml})
 * and run {@code ./gradlew :RDQ:rdq-common:check} so the JEConfig mapper validates
 * field bindings before the changes reach production.
 * </p>
 */
package com.raindropcentral.rdq.config;
