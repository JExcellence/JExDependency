/**
 * Repository layer for rank system persistence and data access.
 *
 * <p>Contains repositories for:
 * <ul>
 *   <li>{@link com.raindropcentral.rdq.rank.repository.RankTreeRepository} - In-memory rank tree definitions</li>
 *   <li>{@link com.raindropcentral.rdq.rank.repository.RankRepository} - In-memory rank definitions</li>
 *   <li>{@link com.raindropcentral.rdq.rank.repository.PlayerRankRepository} - JPA persistence for player rank unlocks</li>
 *   <li>{@link com.raindropcentral.rdq.rank.repository.PlayerRankPathRepository} - JPA persistence for active rank paths</li>
 * </ul>
 *
 * @since 6.0.0
 */
package com.raindropcentral.rdq.rank.repository;
