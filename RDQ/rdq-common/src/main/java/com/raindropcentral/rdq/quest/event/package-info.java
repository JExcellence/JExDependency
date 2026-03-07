/**
 * Quest system custom events.
 * <p>
 * This package contains Bukkit events fired during quest operations:
 * <ul>
 *     <li>{@link com.raindropcentral.rdq.quest.event.QuestStartEvent} - 
 *         Fired when a player starts a quest</li>
 *     <li>{@link com.raindropcentral.rdq.quest.event.QuestCompleteEvent} - 
 *         Fired when a player completes a quest</li>
 *     <li>{@link com.raindropcentral.rdq.quest.event.QuestAbandonEvent} - 
 *         Fired when a player abandons a quest</li>
 *     <li>{@link com.raindropcentral.rdq.quest.event.TaskCompleteEvent} - 
 *         Fired when a player completes a quest task</li>
 * </ul>
 * </p>
 * <p>
 * All events are called on the main thread and follow the standard Bukkit
 * event pattern with HandlerList support.
 * </p>
 * <p>
 * These events can be listened to by other plugins or internal systems to
 * react to quest progress and completion.
 * </p>
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
package com.raindropcentral.rdq.quest.event;
