/**
 * Server metadata entities shared across player aggregates.
 * <p>
 * {@link com.raindropcentral.core.database.entity.server.RServer} tracks logical server instances via
 * UUID to guarantee stable associations for inventories and other per-server projections. The entity
 * normalizes server names by trimming input and enforcing the
 * {@link com.raindropcentral.core.database.entity.server.RServer#updateServerName(String)} validation
 * path to prevent constraint breaches. Persisted rows act as foreign-key anchors for inventory
 * snapshots and should be created before dependent aggregates are flushed.
 * </p>
 */
package com.raindropcentral.core.database.entity.server;
