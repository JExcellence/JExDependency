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

package com.raindropcentral.rdt.view.town;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Tracks one active town-protection editor session per town.
 *
 * <p>The registry is package-private because it only coordinates the protection views in this
 * package. The same player may re-enter nested protection views while another player is blocked
 * until the owning editor session is released.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
final class TownProtectionEditSessionRegistry {

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private TownProtectionEditSessionRegistry() {
    }

    static synchronized boolean canOpen(final @NotNull UUID townUuid, final @NotNull UUID playerUuid) {
        final Session session = SESSIONS.get(townUuid);
        return session == null || Objects.equals(session.ownerUuid(), playerUuid);
    }

    static synchronized boolean acquire(final @NotNull UUID townUuid, final @NotNull UUID playerUuid) {
        final Session session = SESSIONS.get(townUuid);
        if (session == null) {
            SESSIONS.put(townUuid, new Session(playerUuid, 1));
            return true;
        }
        if (!Objects.equals(session.ownerUuid(), playerUuid)) {
            return false;
        }
        SESSIONS.put(townUuid, new Session(playerUuid, session.holdCount() + 1));
        return true;
    }

    static synchronized void release(final @NotNull UUID townUuid, final @NotNull UUID playerUuid) {
        final Session session = SESSIONS.get(townUuid);
        if (session == null || !Objects.equals(session.ownerUuid(), playerUuid)) {
            return;
        }
        if (session.holdCount() <= 1) {
            SESSIONS.remove(townUuid);
            return;
        }
        SESSIONS.put(townUuid, new Session(playerUuid, session.holdCount() - 1));
    }

    static synchronized void clear() {
        SESSIONS.clear();
    }

    private record Session(@NotNull UUID ownerUuid, int holdCount) {
    }
}
