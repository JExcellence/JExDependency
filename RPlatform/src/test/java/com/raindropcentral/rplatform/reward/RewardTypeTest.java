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

package com.raindropcentral.rplatform.reward;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests type metadata helpers in {@link RewardType}.
 *
 * @author ItsRainingHP
 * @since 2.0.0
 * @version 1.0.0
 */
class RewardTypeTest {

    @Test
    void coreFactoryUsesCorePluginNamespace() {
        final RewardType type = RewardType.core("ITEM", DummyReward.class);

        assertEquals("ITEM", type.id());
        assertEquals("core", type.pluginId());
        assertEquals("core:ITEM", type.getQualifiedName());
    }

    @Test
    void pluginFactoryUsesProvidedPluginNamespace() {
        final RewardType type = RewardType.plugin("BONUS", "rdq", DummyReward.class);

        assertEquals("BONUS", type.id());
        assertEquals("rdq", type.pluginId());
        assertEquals("rdq:BONUS", type.getQualifiedName());
    }

    private static final class DummyReward extends AbstractReward {

        @Override
        public String getTypeId() {
            return "DUMMY";
        }

        @Override
        public CompletableFuture<Boolean> grant(final Player player) {
            return CompletableFuture.completedFuture(Boolean.TRUE);
        }

        @Override
        public double getEstimatedValue() {
            return 0.0D;
        }
    }
}
