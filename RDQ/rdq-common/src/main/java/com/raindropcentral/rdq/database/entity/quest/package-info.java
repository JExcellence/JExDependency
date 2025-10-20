/**
 * Quest catalog entries and per-player quest progression state.
 * <p>
 * {@link com.raindropcentral.rdq.database.entity.quest.RQuest} and
 * {@link com.raindropcentral.rdq.database.entity.quest.RQuestUpgrade} define the scripted quest lines,
 * while {@link com.raindropcentral.rdq.database.entity.quest.RPlayerQuest} and
 * {@link com.raindropcentral.rdq.database.entity.quest.RPlayerQuestRequirementProgress} persist each
 * {@link com.raindropcentral.rdq.database.entity.player.RDQPlayer}'s progress toward completion and
 * upgrade unlocks. Requirement fields lean on the JSON converters so repositories can translate
 * in-game objectives into persisted progress bars during the repository wiring phase.
 * </p>
 */
package com.raindropcentral.rdq.database.entity.quest;
