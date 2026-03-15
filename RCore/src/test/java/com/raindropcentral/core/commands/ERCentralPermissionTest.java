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
    }
}
