/**
 * Jackson helpers that serialize and deserialize quest and rank requirements.
 * <p>
 * {@link com.raindropcentral.rdq.database.json.requirement.RequirementParser} configures a shared
 * {@link com.fasterxml.jackson.databind.ObjectMapper} with Bukkit-friendly serializers such as
 * {@link com.raindropcentral.rdq.database.json.serializer.ItemStackJSONSerializer} and the
 * {@link com.raindropcentral.rdq.database.json.requirement.RequirementMixin} polymorphic mix-in. The
 * converters apply these utilities so requirement graphs embedded in entities like
 * {@link com.raindropcentral.rdq.database.entity.quest.RQuestUpgradeRequirement} persist cleanly
 * between the view initialization and repository wiring lifecycle stages described in
 * {@link com.raindropcentral.rdq.RDQ#onEnable()}.
 * </p>
 */
package com.raindropcentral.rdq.database.json.requirement;
