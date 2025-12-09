/**
 * Requirement configuration hierarchy.
 * <p>
 * {@link com.raindropcentral.rdq2.config.requirement.RequirementSection} and related classes represent the
 * serialized form of {@link com.raindropcentral.rdq2.requirement.AbstractRequirement} graphs before they are materialised
 * by {@link com.raindropcentral.rdq2.utility.requirement.RequirementFactory}. Factory conversion emits JSON that is parsed by
 * {@link com.raindropcentral.rdq2.database.json.requirement.RequirementParser} to instantiate runtime requirement types.
 * </p>
 * <p>
 * The YAML sources for these sections are embedded in rank definitions ({@code rank/paths/*.yml}) and perk definitions
 * ({@code perks/*.yml}). Whenever requirement fields change, update both the YAML and any persistence serializers, then run
 * {@code ./gradlew :RDQ:rdq-common:check} or execute the requirement factory tests to verify parsing still succeeds.
 * </p>
 */
package com.raindropcentral.rdq2.config.requirement;
