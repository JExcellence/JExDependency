/**
 * Perk configuration definitions.
 * <p>
 * {@link com.raindropcentral.rdq.config.perk.PerkSection} and its nested sections describe each perk's
 * metadata, requirements, and rewards before they are persisted via
 * {@link com.raindropcentral.rdq.database.converter.PerkSectionConverter} and evaluated during gameplay.
 * Source YAML files reside in {@code rdq-common/src/main/resources/perks/}.
 * </p>
 * <p>
 * When adjusting perk configuration models, update the matching YAML schema and re-run
 * {@code ./gradlew :RDQ:rdq-common:check} to exercise the converters against the new shape.
 * </p>
 */
package com.raindropcentral.rdq.config.perk;
