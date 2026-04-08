/**
 * Repository layer for JExOneblock database operations.
 * 
 * <p>This package contains repository implementations that extend JEHibernate's 
 * {@link de.jexcellence.hibernate.repository.CachedRepository} to provide:
 * 
 * <ul>
 *   <li>Cached CRUD operations with automatic cache management</li>
 *   <li>Asynchronous database operations via CompletableFuture</li>
 *   <li>Type-safe query methods with proper null handling</li>
 *   <li>Specialized finder methods returning T (nullable) or Optional&lt;T&gt;</li>
 * </ul>
 * 
 * <h2>Repository Pattern</h2>
 * <p>All repositories follow a consistent pattern:
 * <ul>
 *   <li>Synchronous methods return T (nullable) or List&lt;T&gt;</li>
 *   <li>Asynchronous methods return CompletableFuture&lt;Optional&lt;T&gt;&gt; or CompletableFuture&lt;List&lt;T&gt;&gt;</li>
 *   <li>Methods use {@code @Nullable} and {@code @NotNull} annotations for clarity</li>
 * </ul>
 * 
 * <h2>Available Repositories</h2>
 * <ul>
 *   <li>{@link de.jexcellence.oneblock.database.repository.OneblockPlayerRepository} - Player data</li>
 *   <li>{@link de.jexcellence.oneblock.database.repository.OneblockIslandRepository} - Island data</li>
 *   <li>{@link de.jexcellence.oneblock.database.repository.OneblockIslandMemberRepository} - Island membership</li>
 *   <li>{@link de.jexcellence.oneblock.database.repository.OneblockIslandBanRepository} - Island bans</li>
 *   <li>{@link de.jexcellence.oneblock.database.repository.OneblockEvolutionRepository} - Evolution definitions</li>
 *   <li>{@link de.jexcellence.oneblock.database.repository.OneblockRegionRepository} - Region data</li>
 *   <li>{@link de.jexcellence.oneblock.database.repository.OneblockVisitorSettingsRepository} - Visitor settings</li>
 *   <li>{@link de.jexcellence.oneblock.database.repository.EvolutionBlockRepository} - Evolution blocks</li>
 *   <li>{@link de.jexcellence.oneblock.database.repository.EvolutionEntityRepository} - Evolution entities</li>
 *   <li>{@link de.jexcellence.oneblock.database.repository.EvolutionItemRepository} - Evolution items</li>
 *   <li>{@link de.jexcellence.oneblock.database.repository.IslandInfrastructureRepository} - Infrastructure data</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Synchronous - returns T (nullable)
 * OneblockPlayer player = playerRepository.findByUuid(uuid);
 * if (player != null) {
 *     // use player
 * }
 * 
 * // Asynchronous - returns CompletableFuture<Optional<T>>
 * playerRepository.findByUuidAsync(uuid)
 *     .thenAccept(opt -> opt.ifPresent(p -> {
 *         // use player
 *     }));
 * }</pre>
 * 
 * @author JExcellence
 * @since 2.0.0
 * @version 2.0.0
 */
package de.jexcellence.oneblock.database.repository;
