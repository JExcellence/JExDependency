package com.raindropcentral.rplatform.progression.model;

import com.raindropcentral.rplatform.progression.IProgressionNode;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ProgressionState} record.
 *
 * @author RaindropCentral
 * @version 1.0.0
 * @since 1.0.0
 */
class ProgressionStateTest {
    
    /**
     * Test implementation of IProgressionNode.
     */
    private static class TestNode implements IProgressionNode<TestNode> {
        private final String identifier;
        
        TestNode(String identifier) {
            this.identifier = identifier;
        }
        
        @Override
        public @NotNull String getIdentifier() {
            return identifier;
        }
        
        @Override
        public @NotNull List<String> getPreviousNodeIdentifiers() {
            return List.of();
        }
        
        @Override
        public @NotNull List<String> getNextNodeIdentifiers() {
            return List.of();
        }
    }
    
    @Test
    void testLockedState() {
        // Given: A locked state with missing prerequisites
        TestNode node = new TestNode("test");
        List<String> missing = List.of("prereq1", "prereq2");
        ProgressionState<TestNode> state = new ProgressionState<>(node, ProgressionStatus.LOCKED, missing);
        
        // When & Then: Should be locked
        assertTrue(state.isLocked());
        assertFalse(state.isUnlocked());
        assertFalse(state.canStart());
        assertFalse(state.isActive());
        assertFalse(state.isCompleted());
        assertTrue(state.hasMissingPrerequisites());
        assertEquals(2, state.getMissingPrerequisiteCount());
        assertEquals(missing, state.missingPrerequisites());
    }
    
    @Test
    void testAvailableState() {
        // Given: An available state with no missing prerequisites
        TestNode node = new TestNode("test");
        ProgressionState<TestNode> state = new ProgressionState<>(node, ProgressionStatus.AVAILABLE, List.of());
        
        // When & Then: Should be available
        assertFalse(state.isLocked());
        assertTrue(state.isUnlocked());
        assertTrue(state.canStart());
        assertFalse(state.isActive());
        assertFalse(state.isCompleted());
        assertFalse(state.hasMissingPrerequisites());
        assertEquals(0, state.getMissingPrerequisiteCount());
    }
    
    @Test
    void testActiveState() {
        // Given: An active state
        TestNode node = new TestNode("test");
        ProgressionState<TestNode> state = new ProgressionState<>(node, ProgressionStatus.ACTIVE, List.of());
        
        // When & Then: Should be active
        assertFalse(state.isLocked());
        assertTrue(state.isUnlocked());
        assertFalse(state.canStart());
        assertTrue(state.isActive());
        assertFalse(state.isCompleted());
    }
    
    @Test
    void testCompletedState() {
        // Given: A completed state
        TestNode node = new TestNode("test");
        ProgressionState<TestNode> state = new ProgressionState<>(node, ProgressionStatus.COMPLETED, List.of());
        
        // When & Then: Should be completed
        assertFalse(state.isLocked());
        assertTrue(state.isUnlocked());
        assertFalse(state.canStart());
        assertFalse(state.isActive());
        assertTrue(state.isCompleted());
    }
    
    @Test
    void testGetNodeIdentifier() {
        // Given: A state with a node
        TestNode node = new TestNode("my-node-id");
        ProgressionState<TestNode> state = new ProgressionState<>(node, ProgressionStatus.AVAILABLE, List.of());
        
        // When & Then: Should return node identifier
        assertEquals("my-node-id", state.getNodeIdentifier());
        assertEquals(node.getIdentifier(), state.getNodeIdentifier());
    }
    
    @Test
    void testImmutability_MissingPrerequisites() {
        // Given: A mutable list
        List<String> mutableList = new java.util.ArrayList<>();
        mutableList.add("prereq1");
        
        TestNode node = new TestNode("test");
        ProgressionState<TestNode> state = new ProgressionState<>(node, ProgressionStatus.LOCKED, mutableList);
        
        // When: Modifying the original list
        mutableList.add("prereq2");
        
        // Then: State should not be affected (defensive copy)
        assertEquals(1, state.getMissingPrerequisiteCount());
        assertEquals(List.of("prereq1"), state.missingPrerequisites());
    }
    
    @Test
    void testNullNode_ThrowsException() {
        // When & Then: Null node should throw exception
        assertThrows(NullPointerException.class, () ->
            new ProgressionState<>(null, ProgressionStatus.AVAILABLE, List.of())
        );
    }
    
    @Test
    void testNullStatus_ThrowsException() {
        // Given: A valid node
        TestNode node = new TestNode("test");
        
        // When & Then: Null status should throw exception
        assertThrows(NullPointerException.class, () ->
            new ProgressionState<>(node, null, List.of())
        );
    }
    
    @Test
    void testNullMissingPrerequisites_ThrowsException() {
        // Given: A valid node
        TestNode node = new TestNode("test");
        
        // When & Then: Null missing prerequisites should throw exception
        assertThrows(NullPointerException.class, () ->
            new ProgressionState<>(node, ProgressionStatus.AVAILABLE, null)
        );
    }
    
    @Test
    void testRecordEquality() {
        // Given: Two states with the same node instance
        TestNode node = new TestNode("test");
        ProgressionState<TestNode> state1 = new ProgressionState<>(node, ProgressionStatus.AVAILABLE, List.of());
        ProgressionState<TestNode> state2 = new ProgressionState<>(node, ProgressionStatus.AVAILABLE, List.of());
        
        // When & Then: Should be equal (records use structural equality)
        assertEquals(state1, state2);
        assertEquals(state1.hashCode(), state2.hashCode());
    }
    
    @Test
    void testRecordInequality() {
        // Given: Two states with different nodes
        TestNode node1 = new TestNode("test1");
        TestNode node2 = new TestNode("test2");
        ProgressionState<TestNode> state1 = new ProgressionState<>(node1, ProgressionStatus.AVAILABLE, List.of());
        ProgressionState<TestNode> state2 = new ProgressionState<>(node2, ProgressionStatus.AVAILABLE, List.of());
        
        // When & Then: Should not be equal
        assertNotEquals(state1, state2);
    }
    
    @Test
    void testRecordToString() {
        // Given: A state
        TestNode node = new TestNode("test");
        ProgressionState<TestNode> state = new ProgressionState<>(node, ProgressionStatus.LOCKED, List.of("prereq1"));
        
        // When: Getting string representation
        String str = state.toString();
        
        // Then: Should contain all fields
        assertTrue(str.contains("LOCKED"));
        assertTrue(str.contains("prereq1"));
    }
}
