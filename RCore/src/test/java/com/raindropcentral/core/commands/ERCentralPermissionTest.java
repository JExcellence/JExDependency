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

package com.raindropcentral.core.commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests permission-node mappings in {@link ERCentralPermission}.
 *
 * @author ItsRainingHP
 * @since 2.0.0
 * @version 1.0.0
 */
class ERCentralPermissionTest {

    @Test
    void exposesInternalAndFallbackNodes() {
        assertEquals("connect", ERCentralPermission.CONNECT.getInternalName());
        assertEquals("rcore.central.connect", ERCentralPermission.CONNECT.getFallbackNode());

        assertEquals("disconnect", ERCentralPermission.DISCONNECT.getInternalName());
        assertEquals("rcore.central.disconnect", ERCentralPermission.DISCONNECT.getFallbackNode());

        assertEquals("claimDroplets", ERCentralPermission.CLAIM_DROPLETS.getInternalName());
        assertEquals("rcore.central.claim.droplets", ERCentralPermission.CLAIM_DROPLETS.getFallbackNode());
    }
}
