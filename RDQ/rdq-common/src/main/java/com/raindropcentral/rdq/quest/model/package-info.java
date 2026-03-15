/**
 * Quest system domain models, DTOs, and value objects.
 *
 * <p>This package contains immutable data transfer objects and value objects used
 * throughout the quest system:
 * <ul>
 *     <li>{@link com.raindropcentral.rdq.quest.model.QuestDifficulty} - Quest difficulty enum</li>
 *     <li>{@link com.raindropcentral.rdq.quest.model.ActiveQuest} - Active quest DTO</li>
 *     <li>{@link com.raindropcentral.rdq.quest.model.TaskProgress} - Task progress DTO</li>
 *     <li>{@link com.raindropcentral.rdq.quest.model.QuestProgress} - Quest progress DTO</li>
 *     <li>{@link com.raindropcentral.rdq.quest.model.QuestStartResult} - Quest start result (sealed interface)</li>
 *     <li>{@link com.raindropcentral.rdq.quest.model.QuestAbandonResult} - Quest abandon result (sealed interface)</li>
 * </ul>
 *
 * <p>All DTOs are implemented as Java records for immutability and conciseness.
 * Result types use sealed interfaces for type-safe pattern matching.
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
package com.raindropcentral.rdq.quest.model;
