/**
 * Quest system service layer.
 * <p>
 * This package contains service interfaces and implementations for quest management:
 * <ul>
 *     <li>{@link com.raindropcentral.rdq.quest.service.QuestService} - Quest management service interface</li>
 *     <li>{@link com.raindropcentral.rdq.quest.service.QuestServiceImpl} - Quest service implementation</li>
 * </ul>
 * </p>
 * <p>
 * The service layer provides high-level quest operations including:
 * <ul>
 *     <li>Quest discovery and browsing</li>
 *     <li>Quest starting with validation</li>
 *     <li>Quest abandoning</li>
 *     <li>Active quest tracking</li>
 *     <li>Progress monitoring</li>
 *     <li>Requirement checking</li>
 * </ul>
 * </p>
 * <p>
 * All service methods return {@link java.util.concurrent.CompletableFuture} for
 * non-blocking async operations. The implementation uses Caffeine caching for
 * improved performance on frequently accessed data.
 * </p>
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
package com.raindropcentral.rdq.quest.service;
