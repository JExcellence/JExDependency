/**
 * Quest system database entities.
 * <p>
 * This package contains JPA entities for the RDQ quest system, including:
 * <ul>
 *     <li>{@link com.raindropcentral.rdq.database.entity.quest.QuestCategory} - Quest category groupings</li>
 *     <li>{@link com.raindropcentral.rdq.database.entity.quest.Quest} - Quest definitions</li>
 *     <li>{@link com.raindropcentral.rdq.database.entity.quest.QuestTask} - Individual quest tasks</li>
 *     <li>{@link com.raindropcentral.rdq.database.entity.quest.QuestUser} - Player quest progress</li>
 *     <li>{@link com.raindropcentral.rdq.database.entity.quest.QuestTaskProgress} - Player task progress</li>
 *     <li>{@link com.raindropcentral.rdq.database.entity.quest.QuestCompletionHistory} - Quest completion tracking</li>
 * </ul>
 * <p>
 * All entities extend {@link de.jexcellence.hibernate.entity.BaseEntity} and use JPA annotations
 * for persistence with JExHibernate.
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
package com.raindropcentral.rdq.database.entity.quest;
