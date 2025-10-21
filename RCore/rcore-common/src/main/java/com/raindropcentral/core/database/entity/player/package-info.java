/**
 * Player aggregate roots and supporting utilities.
 * <p>
 * {@link com.raindropcentral.core.database.entity.player.RPlayer} represents the canonical identity
 * for a Minecraft profile. It enforces name length constraints before persistence and guarantees
 * uniqueness through the {@code uniqueId} natural identifier. When attaching statistics, always use
 * {@link com.raindropcentral.core.database.entity.player.RPlayer#setPlayerStatistic(com.raindropcentral.core.database.entity.statistic.RPlayerStatistic)}
 * to synchronize both sides of the relationship and avoid detached statistic graphs.
 * </p>
 * <p>
 * Serialization concerns are minimal beyond UUID handling, however name updates should be staged via
 * {@link com.raindropcentral.core.database.entity.player.RPlayer#updatePlayerName(String)} so that
 * validation mirrors the constructor logic and repositories can safely invoke {@code merge} without
 * risking constraint violations.
 * </p>
 */
package com.raindropcentral.core.database.entity.player;
