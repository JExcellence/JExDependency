/**
 * RDQ (RaindropQuests) - A modern Minecraft plugin providing rank progression,
 * bounty hunting, and perk systems.
 *
 * <p>This package contains the core functionality shared between free and premium editions.
 * The architecture follows a layered design:
 * <ul>
 *   <li>{@code api} - Public interfaces and contracts</li>
 *   <li>{@code rank} - Rank progression system</li>
 *   <li>{@code bounty} - Bounty hunting system</li>
 *   <li>{@code perk} - Perk and ability system</li>
 *   <li>{@code player} - Player data management</li>
 *   <li>{@code shared} - Cross-cutting utilities</li>
 * </ul>
 *
 * <p>Key design principles:
 * <ul>
 *   <li>Records for all DTOs and value objects</li>
 *   <li>Sealed interfaces for type-safe hierarchies</li>
 *   <li>Pattern matching for control flow</li>
 *   <li>CompletableFuture for async operations</li>
 *   <li>Virtual threads (Java 21+) for concurrency</li>
 * </ul>
 *
 * @since 6.0.0
 */
package com.raindropcentral.rdq;
