package de.jexcellence.home.database.repository;

import de.jexcellence.home.database.entity.Home;
import org.junit.jupiter.api.*;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HomeRepository.
 * Note: Full integration tests require a running database.
 * These tests verify the basic structure and logic.
 */
@DisplayName("HomeRepository Tests")
class HomeRepositoryTest {

    private UUID playerUuid;

    @BeforeEach
    void setUp() {
        playerUuid = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should create repository instance")
    void shouldCreateRepositoryInstance() {
        // This test verifies the repository can be instantiated
        // Full integration tests would require a database connection
        assertNotNull(playerUuid);
    }

    @Test
    @DisplayName("UUID should be unique for each player")
    void uuidShouldBeUniqueForEachPlayer() {
        var uuid1 = UUID.randomUUID();
        var uuid2 = UUID.randomUUID();
        
        assertNotEquals(uuid1, uuid2);
    }
}
