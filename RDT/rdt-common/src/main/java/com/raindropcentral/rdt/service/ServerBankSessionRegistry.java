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

package com.raindropcentral.rdt.service;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * Tracks one active viewer for the singleton admin server-bank UI.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
final class ServerBankSessionRegistry {

    private static Session session;

    private ServerBankSessionRegistry() {
    }

    static synchronized boolean acquire(final @NotNull UUID ownerUuid) {
        if (session == null) {
            session = new Session(ownerUuid, 1);
            return true;
        }
        if (!Objects.equals(session.ownerUuid(), ownerUuid)) {
            return false;
        }
        session = new Session(ownerUuid, session.holdCount() + 1);
        return true;
    }

    static synchronized void release(final @NotNull UUID ownerUuid) {
        if (session == null || !Objects.equals(session.ownerUuid(), ownerUuid)) {
            return;
        }
        if (session.holdCount() <= 1) {
            session = null;
            return;
        }
        session = new Session(ownerUuid, session.holdCount() - 1);
    }

    static synchronized boolean isLocked() {
        return session != null;
    }

    static synchronized void clear() {
        session = null;
    }

    private record Session(@NotNull UUID ownerUuid, int holdCount) {
    }
}
