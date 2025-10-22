package com.raindropcentral.rdq.api.spi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class PersistenceRegistryTest {

    @Mock
    private BountyPersistence bountyPersistence;

    @Mock
    private PlayerPersistence playerPersistence;

    @Test
    void builderExposesConfiguredAdapters() {
        PersistenceRegistry registry = PersistenceRegistry.builder()
            .bountyPersistence(bountyPersistence)
            .playerPersistence(playerPersistence)
            .build();

        assertSame(bountyPersistence, registry.getBountyPersistence().orElseThrow());
        assertSame(playerPersistence, registry.getPlayerPersistence().orElseThrow());
    }

    @Test
    void builderClearsAdaptersWhenNullAssignmentsProvided() {
        PersistenceRegistry.Builder builder = PersistenceRegistry.builder();

        PersistenceRegistry emptyRegistry = builder.build();
        assertTrue(emptyRegistry.getBountyPersistence().isEmpty());
        assertTrue(emptyRegistry.getPlayerPersistence().isEmpty());

        builder.bountyPersistence(bountyPersistence)
            .playerPersistence(playerPersistence)
            .bountyPersistence(null)
            .playerPersistence(null);

        PersistenceRegistry clearedRegistry = builder.build();
        assertTrue(clearedRegistry.getBountyPersistence().isEmpty());
        assertTrue(clearedRegistry.getPlayerPersistence().isEmpty());
    }
}
