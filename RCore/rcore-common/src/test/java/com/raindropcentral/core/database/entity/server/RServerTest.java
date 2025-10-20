package com.raindropcentral.core.database.entity.server;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RServerTest {

    @Test
    void constructorShouldRejectNullArguments() {
        assertThrows(NullPointerException.class, () -> new RServer(null, "Alpha"));
        assertThrows(NullPointerException.class, () -> new RServer(UUID.randomUUID(), null));
    }

    @Test
    void constructorShouldTrimAndExposeValues() {
        final UUID uniqueId = UUID.randomUUID();
        final RServer server = new RServer(uniqueId, "  Alpha  ");

        assertEquals(uniqueId, server.getUniqueId());
        assertEquals("Alpha", server.getServerName());
    }

    @Test
    void constructorShouldEnforceNameLengthBounds() {
        final UUID uniqueId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () -> new RServer(uniqueId, ""));
        assertThrows(IllegalArgumentException.class, () -> new RServer(uniqueId, "   "));
        assertThrows(IllegalArgumentException.class, () -> new RServer(uniqueId, "a".repeat(51)));
    }

    @Test
    void updateServerNameShouldTrimAndApplyWhenValid() {
        final RServer server = new RServer(UUID.randomUUID(), "Alpha");

        server.updateServerName("  Beta  ");

        assertEquals("Beta", server.getServerName());
    }

    @Test
    void updateServerNameShouldRejectInvalidInputs() {
        final RServer server = new RServer(UUID.randomUUID(), "Alpha");

        assertThrows(NullPointerException.class, () -> server.updateServerName(null));
        assertThrows(IllegalArgumentException.class, () -> server.updateServerName(" "));
        assertThrows(IllegalArgumentException.class, () -> server.updateServerName("a".repeat(51)));
    }

    @Test
    void toStringShouldIncludeIdentifiersForDiagnostics() {
        final RServer server = new RServer(UUID.randomUUID(), "Alpha");
        final Long identifier = server.getId();

        final String description = server.toString();

        assertTrue(description.contains("id=" + identifier));
        assertTrue(description.contains(server.getUniqueId().toString()));
        assertTrue(description.contains(server.getServerName()));
    }
}
