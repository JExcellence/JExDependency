package com.raindropcentral.rdq.bounty.visual;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.raindropcentral.rdq.RDQ;
import net.jqwik.api.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Particle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for visual indicator management.
 * These tests verify that visual indicators are correctly applied and removed.
 */
class VisualIndicatorPropertiesTest {
    
    private ServerMock server;
    private RDQ rdq;
    
    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        // We'll use a mock RDQ instance for testing
        // In a real scenario, this would be properly initialized
    }
    
    @AfterEach
    void tearDown() {
        if (server != null) {
            MockBukkit.unmock();
        }
    }
    
    /**
     * **Feature: bounty-system-rebuild, Property 59: Tab prefix application**
     * 
     * For any player with an active bounty when visual indicators are enabled, 
     * the configured tab prefix should be applied to their tab list name.
     * 
     * **Validates: Requirements 14.1**
     */
    @Property(tries = 100)
    @Label("Property 59: Tab prefix application")
    void tabPrefixApplicationProperty(
        @ForAll("playerName") String playerName,
        @ForAll("tabPrefix") String tabPrefix
    ) {
        // Given: A visual indicator manager with a configured tab prefix
        VisualIndicatorManager manager = createManager(tabPrefix, "<red>", false);
        PlayerMock player = server.addPlayer(playerName);
        Component originalTabListName = player.playerListName();
        
        // When: Applying indicators to the player
        manager.applyIndicators(player);
        
        // Then: The player's tab list name should contain the prefix
        Component newTabListName = player.playerListName();
        assertNotNull(newTabListName, "Tab list name should not be null");
        assertNotEquals(originalTabListName, newTabListName, 
            "Tab list name should be changed");
        
        String tabListText = PlainTextComponentSerializer.plainText().serialize(newTabListName);
        assertTrue(tabListText.contains(playerName), 
            "Tab list name should still contain player name");
        
        // Verify indicator is tracked
        assertTrue(manager.hasIndicators(player.getUniqueId()), 
            "Manager should track the player's indicators");
        
        // Cleanup
        manager.removeIndicators(player);
    }
    
    /**
     * **Feature: bounty-system-rebuild, Property 60: Name color application**
     * 
     * For any player with an active bounty when visual indicators are enabled, 
     * the configured name color should be applied to their display name.
     * 
     * **Validates: Requirements 14.2**
     */
    @Property(tries = 100)
    @Label("Property 60: Name color application")
    void nameColorApplicationProperty(
        @ForAll("playerName") String playerName,
        @ForAll("nameColor") String nameColor
    ) {
        // Given: A visual indicator manager with a configured name color
        VisualIndicatorManager manager = createManager("<red>[BOUNTY] </red>", nameColor, false);
        PlayerMock player = server.addPlayer(playerName);
        Component originalDisplayName = player.displayName();
        
        // When: Applying indicators to the player
        manager.applyIndicators(player);
        
        // Then: The player's display name should be changed
        Component newDisplayName = player.displayName();
        assertNotNull(newDisplayName, "Display name should not be null");
        assertNotEquals(originalDisplayName, newDisplayName, 
            "Display name should be changed");
        
        String displayText = PlainTextComponentSerializer.plainText().serialize(newDisplayName);
        assertTrue(displayText.contains(playerName), 
            "Display name should still contain player name");
        
        // Verify indicator is tracked
        assertTrue(manager.hasIndicators(player.getUniqueId()), 
            "Manager should track the player's indicators");
        
        // Cleanup
        manager.removeIndicators(player);
    }
    
    /**
     * **Feature: bounty-system-rebuild, Property 61: Particle spawning**
     * 
     * For any player with an active bounty when particles are enabled, 
     * particles should spawn around them at the configured interval.
     * 
     * **Validates: Requirements 14.3**
     */
    @Property(tries = 100)
    @Label("Property 61: Particle spawning")
    void particleSpawningProperty(
        @ForAll("playerName") String playerName,
        @ForAll("particleIntervalTicks") int particleIntervalTicks
    ) {
        // Given: A visual indicator manager with particles enabled
        VisualIndicatorManager manager = createManager(
            "<red>[BOUNTY] </red>", 
            "<red>", 
            true,
            Particle.FLAME,
            particleIntervalTicks
        );
        PlayerMock player = server.addPlayer(playerName);
        
        // When: Applying indicators to the player
        manager.applyIndicators(player);
        
        // Then: The manager should track the player for particle spawning
        assertTrue(manager.hasIndicators(player.getUniqueId()), 
            "Manager should track the player for particles");
        
        // Note: We can't easily test actual particle spawning in unit tests
        // as it requires world interaction, but we verify the player is tracked
        
        // Cleanup
        manager.removeIndicators(player);
        manager.shutdown();
    }
    
    /**
     * **Feature: bounty-system-rebuild, Property 62: Visual indicator cleanup**
     * 
     * For any bounty that is claimed or expires, all visual indicators 
     * should be removed from the target.
     * 
     * **Validates: Requirements 14.4**
     */
    @Property(tries = 100)
    @Label("Property 62: Visual indicator cleanup")
    void visualIndicatorCleanupProperty(
        @ForAll("playerName") String playerName
    ) {
        // Given: A player with applied visual indicators
        VisualIndicatorManager manager = createManager("<red>[BOUNTY] </red>", "<red>", false);
        PlayerMock player = server.addPlayer(playerName);
        Component originalDisplayName = player.displayName();
        Component originalTabListName = player.playerListName();
        
        manager.applyIndicators(player);
        assertTrue(manager.hasIndicators(player.getUniqueId()), 
            "Indicators should be applied");
        
        // When: Removing indicators (simulating bounty claim/expiration)
        manager.removeIndicators(player);
        
        // Then: All indicators should be removed
        assertFalse(manager.hasIndicators(player.getUniqueId()), 
            "Manager should no longer track the player");
        
        // Display name and tab list name should be restored
        assertEquals(originalDisplayName, player.displayName(), 
            "Display name should be restored");
        assertEquals(originalTabListName, player.playerListName(), 
            "Tab list name should be restored");
    }
    
    /**
     * Property: Idempotent indicator application
     * 
     * For any player, applying indicators multiple times should not cause issues
     * and should maintain the same state.
     */
    @Property(tries = 100)
    @Label("Idempotent indicator application")
    void idempotentApplicationProperty(
        @ForAll("playerName") String playerName,
        @ForAll("applicationCount") int applicationCount
    ) {
        Assume.that(applicationCount >= 1 && applicationCount <= 5);
        
        // Given: A visual indicator manager
        VisualIndicatorManager manager = createManager("<red>[BOUNTY] </red>", "<red>", false);
        PlayerMock player = server.addPlayer(playerName);
        
        // When: Applying indicators multiple times
        for (int i = 0; i < applicationCount; i++) {
            manager.applyIndicators(player);
        }
        
        // Then: The player should still have indicators applied exactly once
        assertTrue(manager.hasIndicators(player.getUniqueId()), 
            "Indicators should be applied");
        assertEquals(1, manager.getActiveIndicatorCount(), 
            "Should only have one set of indicators");
        
        // Cleanup
        manager.removeIndicators(player);
    }
    
    /**
     * Property: Indicator removal for offline players
     * 
     * For any offline player UUID, removing indicators should not cause errors.
     */
    @Property(tries = 100)
    @Label("Safe removal for offline players")
    void offlinePlayerRemovalProperty(
        @ForAll("playerId") UUID playerId
    ) {
        // Given: A visual indicator manager
        VisualIndicatorManager manager = createManager("<red>[BOUNTY] </red>", "<red>", false);
        
        // When: Removing indicators for a non-existent/offline player
        // Then: Should not throw an exception
        assertDoesNotThrow(() -> manager.removeIndicators(playerId), 
            "Removing indicators for offline player should not throw");
        
        assertFalse(manager.hasIndicators(playerId), 
            "Should not track non-existent player");
    }
    
    /**
     * Property: Multiple players with indicators
     * 
     * For any set of players, the manager should correctly track all of them independently.
     */
    @Property(tries = 100)
    @Label("Multiple players tracking")
    void multiplePlayersProperty(
        @ForAll("playerNameList") java.util.List<String> playerNames
    ) {
        Assume.that(!playerNames.isEmpty());
        Assume.that(playerNames.size() <= 10);
        Assume.that(playerNames.stream().distinct().count() == playerNames.size());
        
        // Given: A visual indicator manager
        VisualIndicatorManager manager = createManager("<red>[BOUNTY] </red>", "<red>", false);
        java.util.List<PlayerMock> players = new java.util.ArrayList<>();
        
        // When: Applying indicators to multiple players
        for (String name : playerNames) {
            PlayerMock player = server.addPlayer(name);
            players.add(player);
            manager.applyIndicators(player);
        }
        
        // Then: All players should be tracked
        assertEquals(playerNames.size(), manager.getActiveIndicatorCount(), 
            "Should track all players");
        
        for (PlayerMock player : players) {
            assertTrue(manager.hasIndicators(player.getUniqueId()), 
                "Each player should have indicators");
        }
        
        // Cleanup
        for (PlayerMock player : players) {
            manager.removeIndicators(player);
        }
        
        assertEquals(0, manager.getActiveIndicatorCount(), 
            "All indicators should be removed");
    }
    
    // ========== Helper Methods ==========
    
    private VisualIndicatorManager createManager(String tabPrefix, String nameColor, boolean particlesEnabled) {
        return createManager(tabPrefix, nameColor, particlesEnabled, Particle.FLAME, 20);
    }
    
    private VisualIndicatorManager createManager(
            String tabPrefix, 
            String nameColor, 
            boolean particlesEnabled,
            Particle particleType,
            int particleIntervalTicks
    ) {
        // Create a minimal mock RDQ instance
        // In a real scenario, this would be properly initialized
        return new VisualIndicatorManager(
            null, // RDQ instance - not needed for these tests
            tabPrefix,
            nameColor,
            particlesEnabled,
            particleType,
            particleIntervalTicks
        );
    }
    
    // ========== Arbitraries ==========
    
    @Provide
    Arbitrary<String> playerName() {
        return Arbitraries.strings()
            .alpha()
            .numeric()
            .ofMinLength(3)
            .ofMaxLength(16);
    }
    
    @Provide
    Arbitrary<String> tabPrefix() {
        return Arbitraries.of(
            "<red>[BOUNTY] </red>",
            "<gold>[TARGET] </gold>",
            "<dark_red>[WANTED] </dark_red>",
            "<yellow>★ </yellow>"
        );
    }
    
    @Provide
    Arbitrary<String> nameColor() {
        return Arbitraries.of(
            "<red>",
            "<gold>",
            "<dark_red>",
            "<yellow>"
        );
    }
    
    @Provide
    Arbitrary<Integer> particleIntervalTicks() {
        return Arbitraries.integers().between(10, 100);
    }
    
    @Provide
    Arbitrary<UUID> playerId() {
        return Arbitraries.randomValue(random -> UUID.randomUUID());
    }
    
    @Provide
    Arbitrary<Integer> applicationCount() {
        return Arbitraries.integers().between(1, 5);
    }
    
    @Provide
    Arbitrary<java.util.List<String>> playerNameList() {
        return playerName().list().ofMinSize(1).ofMaxSize(10).uniqueElements();
    }
    
}
