/**
 * Quest system cache management.
 * <p>
 * This package contains cache management classes for player quest data:
 * <ul>
 *     <li>{@link com.raindropcentral.rdq.quest.cache.QuestCacheManager} - 
 *         Manages in-memory cache of player quest data</li>
 *     <li>{@link com.raindropcentral.rdq.quest.cache.QuestCacheListener} - 
 *         Handles cache loading/saving on player join/quit</li>
 * </ul>
 * </p>
 * <p>
 * The cache system follows the SimplePerkCache pattern:
 * <ul>
 *     <li>Load all active quests on player join</li>
 *     <li>Store in memory for instant access</li>
 *     <li>Save on player quit</li>
 *     <li>Auto-save every 5 minutes for crash protection</li>
 * </ul>
 * </p>
 * <p>
 * Thread Safety: All cached lists are wrapped in synchronized lists to prevent
 * ConcurrentModificationException when data is modified while auto-save is running.
 * </p>
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
package com.raindropcentral.rdq.quest.cache;
