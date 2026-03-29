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

package com.raindropcentral.rplatform.cookie;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Read-only lookup for active droplet-cookie boosts.
 *
 * <p>The lookup is synchronous and intended for hot-path integration hooks that need to resolve
 * player-specific multipliers without touching persistence.</p>
 */
public interface CookieBoostLookup {

    /**
     * Resolves the effective multiplier for the supplied boost type and target.
     *
     * @param playerId boosted player
     * @param boostType boost family
     * @param integrationId optional integration identifier for target-scoped boosts
     * @param targetId optional target identifier such as a skill or job id
     * @return multiplier to apply, where {@code 1.0} means no active boost
     */
    double getMultiplier(
            @NotNull UUID playerId,
            @NotNull CookieBoostType boostType,
            @Nullable String integrationId,
            @Nullable String targetId
    );

    /**
     * Resolves whether the player currently has an active double-drop boost.
     *
     * @param playerId boosted player
     * @return {@code true} when active
     */
    default boolean hasDoubleDrop(@NotNull UUID playerId) {
        return getMultiplier(playerId, CookieBoostType.DOUBLE_DROP, null, null) > 1.0D;
    }
}
