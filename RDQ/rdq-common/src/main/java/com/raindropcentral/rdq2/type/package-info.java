/**
 * Enumerations that anchor configuration to runtime behaviour.
 * <p>
 * Types such as {@link com.raindropcentral.rdq2.type.EBountyClaimMode} and {@link com.raindropcentral.rdq2.type.EPerkType}
 * provide the canonical identifiers referenced by configuration sections in {@code rdq-common/src/main/resources}.
 * Configuration packages (for example {@link com.raindropcentral.rdq2.config.bounty} and {@link com.raindropcentral.rdq2.config.perk})
 * persist these enum names and runtime logic switches on them when resolving behaviour.
 * </p>
 * <p>
 * When adding enum values, update the consuming YAML (e.g. {@code bounty/bounty.yml}, {@code perks/*.yml}) and any
 * switch statements or factories that interpret the new modes, then execute
 * {@code ./gradlew :RDQ:rdq-common:check} to ensure serialization paths recognise the change.
 * </p>
 */
package com.raindropcentral.rdq2.type;
