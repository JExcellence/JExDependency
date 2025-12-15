/**
 * Runtime requirement implementations.
 * <p>
 * Classes in this package implement {@link com.raindropcentral.rdq.requirement.AbstractRequirement} and are hydrated from
 * configuration sections defined under {@link com.raindropcentral.rdq.config.requirement}. During rank and perk loading,
 * {@link com.raindropcentral.rdq.utility.requirement.RequirementFactory} converts
 * {@link com.raindropcentral.rdq.config.requirement.BaseRequirementSection} graphs into JSON that is parsed via
 * </p>
 * <p>
 * Ensure enum identifiers in {@link com.raindropcentral.rdq.requirement.Requirement.Type} stay aligned with the
 * {@code type} fields used in YAML resources ({@code rank/paths/*.yml}, {@code perks/*.yml}) and the serialization layer.
 * Whenever new requirement capabilities are added, extend the config sections and rerun
 * {@code ./gradlew :RDQ:rdq-common:check} to validate the full mapping pipeline.
 * </p>
 */
package com.raindropcentral.rdq.requirement;
