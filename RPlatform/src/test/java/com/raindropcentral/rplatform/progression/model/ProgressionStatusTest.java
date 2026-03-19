package com.raindropcentral.rplatform.progression.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ProgressionStatus} enum.
 *
 * @author RaindropCentral
 * @version 1.0.0
 * @since 1.0.0
 */
class ProgressionStatusTest {
    
    @Test
    void testLocked_IsNotUnlocked() {
        // Given: LOCKED status
        ProgressionStatus status = ProgressionStatus.LOCKED;
        
        // When & Then: Should not be unlocked
        assertFalse(status.isUnlocked());
        assertFalse(status.canStart());
        assertFalse(status.isActive());
        assertFalse(status.isCompleted());
    }
    
    @Test
    void testAvailable_IsUnlocked() {
        // Given: AVAILABLE status
        ProgressionStatus status = ProgressionStatus.AVAILABLE;
        
        // When & Then: Should be unlocked and startable
        assertTrue(status.isUnlocked());
        assertTrue(status.canStart());
        assertFalse(status.isActive());
        assertFalse(status.isCompleted());
    }
    
    @Test
    void testActive_IsUnlocked() {
        // Given: ACTIVE status
        ProgressionStatus status = ProgressionStatus.ACTIVE;
        
        // When & Then: Should be unlocked and active
        assertTrue(status.isUnlocked());
        assertFalse(status.canStart());
        assertTrue(status.isActive());
        assertFalse(status.isCompleted());
    }
    
    @Test
    void testCompleted_IsUnlocked() {
        // Given: COMPLETED status
        ProgressionStatus status = ProgressionStatus.COMPLETED;
        
        // When & Then: Should be unlocked and completed
        assertTrue(status.isUnlocked());
        assertFalse(status.canStart());
        assertFalse(status.isActive());
        assertTrue(status.isCompleted());
    }
    
    @Test
    void testAllValues_HaveUniqueProperties() {
        // Given: All status values
        ProgressionStatus[] statuses = ProgressionStatus.values();
        
        // When & Then: Each should have unique combination of properties
        assertEquals(4, statuses.length);
        
        // Only LOCKED is not unlocked
        long lockedCount = java.util.Arrays.stream(statuses)
            .filter(s -> !s.isUnlocked())
            .count();
        assertEquals(1, lockedCount);
        
        // Only AVAILABLE can start
        long availableCount = java.util.Arrays.stream(statuses)
            .filter(ProgressionStatus::canStart)
            .count();
        assertEquals(1, availableCount);
        
        // Only ACTIVE is active
        long activeCount = java.util.Arrays.stream(statuses)
            .filter(ProgressionStatus::isActive)
            .count();
        assertEquals(1, activeCount);
        
        // Only COMPLETED is completed
        long completedCount = java.util.Arrays.stream(statuses)
            .filter(ProgressionStatus::isCompleted)
            .count();
        assertEquals(1, completedCount);
    }
}
