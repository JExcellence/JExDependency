/**
 * Contains entity definitions that make up the RCore persistence aggregates.
 * <p>
 * Aggregates follow a player-centric composition: {@link com.raindropcentral.core.database.entity.player.RPlayer}
 * acts as the root for statistics and inventory snapshots while
 * {@link com.raindropcentral.core.database.entity.server.RServer} scopes server-specific state. When
 * assembling aggregates ensure that bidirectional links—such as
 * {@link com.raindropcentral.core.database.entity.statistic.RPlayerStatistic#setPlayer(com.raindropcentral.core.database.entity.player.RPlayer)}
 * and
 * {@link com.raindropcentral.core.database.entity.player.RPlayer#setPlayerStatistic(com.raindropcentral.core.database.entity.statistic.RPlayerStatistic)}—are
 * synchronized before issuing persistence operations.
 * </p>
 * <p>
 * Serialization strategies differ between entity groups and are described in their respective
 * sub-packages. The base entity layer avoids introducing converters so that subtype packages can
 * tailor serialization to their domain (for example, ItemStack conversion vs. statistic value
 * representation).
 * </p>
 */
package com.raindropcentral.core.database.entity;
