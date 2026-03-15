/**
 * Attribute converters that map Bukkit types to Hibernate-friendly column representations.
 *
 * <p><strong>Runtime responsibilities</strong>
 * <p>Each converter implements {@link jakarta.persistence.AttributeConverter}; frequently used types such as
 * {@link org.bukkit.Location} register with {@code autoApply=true} so entity fields serialise without extra
 * annotations, while collection-oriented mappers like {@link ItemStackMapConverter} stay opt-in to avoid
 * surprising joins.【F:RPlatform/src/main/java/com/raindropcentral/rplatform/database/converter/LocationConverter.java†L1-L60】【F:RPlatform/src/main/java/com/raindropcentral/rplatform/database/converter/ItemStackMapConverter.java†L1-L104】
 * This layer lets statistics, perk payloads, and quest metadata persist complex Bukkit objects in a format
 * Hibernate can manage safely.</p>
 *
 * <p><strong>Synchronising with schema updates</strong>
 * <p>When adjusting database schemas or entity fields, review these converters for compatibility. Changes to
 * delimiter formats or serialised JSON structures require corresponding data migration steps so historic rows
 * remain readable; coordinate with the database package to ship migration routines whenever converter output
 * changes.</p>
 */
package com.raindropcentral.rplatform.database.converter;
