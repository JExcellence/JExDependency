/**
 * Core database infrastructure for the RDQ plugin.
 * <p>
 * The packages beneath this root house the Hibernate/JPA entities that mirror gameplay concepts
 * such as player state, bounty boards, perk ownership, rank progress, and quest completion, along
 * with the converters, JSON serializers, and repositories that persist them. Entities extend the
 * shared {@link de.jexcellence.hibernate.entity.AbstractEntity} base class and map directly to the
 * features exposed to players via managers and views (for example {@link com.raindropcentral.rdq.database.entity.bounty.RBounty}
 * tracks bounty assignments while {@link com.raindropcentral.rdq.database.entity.player.RDQPlayer}
 * aggregates rank, perk, and quest relations).
 * </p>
 *
 * <p>
 * Converters and JSON helpers live alongside the entities to bridge Bukkit-centric objects and
 * polymorphic requirement/reward hierarchies into database-friendly formats. Repositories are
 * provisioned during the <em>repository wiring</em> stage of the staged enable pipeline, ensuring the
 * platform and view layers are already established before any persistence access occurs. See
 * {@link com.raindropcentral.rdq.RDQ#onEnable()} for the orchestrated startup sequence that culminates
 * in repository wiring.
 * </p>
 */
package com.raindropcentral.rdq.database;
