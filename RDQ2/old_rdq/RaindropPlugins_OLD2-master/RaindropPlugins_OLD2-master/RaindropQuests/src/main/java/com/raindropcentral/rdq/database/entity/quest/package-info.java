/**
 * Contains entity classes representing quests and quest-related domain objects for the RaindropQuests application.
 * <p>
 * Classes in this package define the structure, attributes, and relationships of quest entities as they are
 * persisted in the database. These entities typically map to database tables and are used by the persistence
 * and service layers to manage quest data, such as creation, progress tracking, and completion.
 * </p>
 *
 * <p>
 * Typical responsibilities of classes in this package include:
 * <ul>
 *   <li>Defining fields and mappings for quest attributes (e.g., objectives, rewards, status)</li>
 *   <li>Specifying relationships to other entities (e.g., users, perks, quest steps)</li>
 *   <li>Annotating constraints and validation rules relevant to quests</li>
 * </ul>
 * </p>
 *
 * <p>
 * These entity classes are intended for use by the application's data access, business logic, and service layers
 * whenever quest-related data needs to be persisted or retrieved.
 * </p>
 */
package com.raindropcentral.rdq.database.entity.quest;