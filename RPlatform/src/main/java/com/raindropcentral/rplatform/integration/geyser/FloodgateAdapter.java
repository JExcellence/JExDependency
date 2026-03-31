/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rplatform.integration.geyser;

import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Adapter that wraps Floodgate API calls to isolate the dependency.
 *
 * <p>This class is only instantiated when Floodgate is available on the classpath,
 * preventing ClassNotFoundException in environments without Floodgate.
 *
 * @author JExcellence
 * @version 1.0.0
 */
class FloodgateAdapter {

    /**
     * Checks if a UUID belongs to a Floodgate (Bedrock) player.
     *
     * @param uuid the UUID to check
     * @return true if the UUID belongs to a Bedrock player
     */
    boolean isFloodgatePlayer(@NotNull UUID uuid) {
        return FloodgateApi.getInstance().isFloodgatePlayer(uuid);
    }

    /**
     * Gets the Bedrock username for a Floodgate player.
     *
     * @param uuid the player's UUID
     * @return the Bedrock username, or null if not a Floodgate player
     */
    @Nullable
    String getBedrockUsername(@NotNull UUID uuid) {
        FloodgatePlayer player = FloodgateApi.getInstance().getPlayer(uuid);
        return player != null ? player.getUsername() : null;
    }

    /**
     * Gets the linked Java UUID for a Bedrock player, if linked.
     *
     * @param uuid the Bedrock player's UUID
     * @return the linked Java UUID, or null if not linked
     */
    @Nullable
    UUID getLinkedJavaUuid(@NotNull UUID uuid) {
        FloodgatePlayer player = FloodgateApi.getInstance().getPlayer(uuid);
        if (player == null || player.getLinkedPlayer() == null) {
            return null;
        }
        return player.getLinkedPlayer().getJavaUniqueId();
    }
}
