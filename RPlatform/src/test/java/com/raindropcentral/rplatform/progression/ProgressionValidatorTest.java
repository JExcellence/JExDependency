package com.raindropcentral.rplatform.progression;

import com.raindropcentral.rplatform.progression.exception.CircularDependencyException;
import com.raindropcentral.rplatform.progression.model.ProgressionState;
import com.raindropcentral.rplatform.progression.model.ProgressionStatus;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for {@link ProgressionValidator}.
 *
 * @author RaindropCentral
 * @version 1.0.0
 * @since 1.0.0
 */
class ProgressionValidatorTest {
    
    /**
     * Test implementation of IProgressionNode for testing purposes.
     */
    private static class TestNode implements IProgressionNode<TestNode> {
        private final String identifier;
        private final List<String> previousNodes;
        private final List<String> nextNodes;
        
        TestNode(String identifier, List<String> previousNodes, List<String> nextNodes) {
            this.identifier = identifier;
            this.previousNodes = previousNodes;
            this.nextNodes = nextNodes;
        }
        
        @Override
        public @NotNull String getIdentifier() {
            return identifier;
        }
        
        @Override
        public @NotNull List<String> getPreviousNodeIdentifiers() {
            return previousNodes;
        }
        
        @Override
        public @NotNull List<String> getNextNodeIdentifiers() {
            return nextNodes;
        }
    }
    
    private MockCompletionTracker<TestNode> tracker;
    private ProgressionValidator<TestNode> validator;
    private UUID playerId;
    
    @BeforeEach
    void setUp() {
        tracker = new MockCompletionTracker<>();
        playerId = UUID.randomUUID();
    }
    
    @Test
    void testLinearProgression() {
        // Setup: A -> B -> C
        TestNode nodeA = new TestNode("node_a", List.of(), List.of("node_b"));
        TestNode nodeB = new TestNode("node_b", List.of("node_a"), List.of("node_c"));
        TestNode nodeC = new TestNode("node_c", List.of("node_b"), List.of());
        
        validator = new ProgressionValidator<>(tracker, List.of(nodeA, nodeB, nodeC));
        
        // Initially, only A is unlocked
        assertTrue(validator.isNodeUnlocked(playerId, "node_a").join());
        assertFalse(validator.isNodeUnlocked(playerId, "node_b").join());
        assertFalse(validator.isNodeUnlocked(playerId, "node_c").join());
        
        // Complete A
        tracker.markCompleted(playerId, "node_a").join();
        
        // Now B is unlocked
        assertTrue(validator.isNodeUnlocked(playerId, "node_b").join());
        assertFalse(validator.isNodeUnlocked(playerId, "node_c").join());
        
        // Complete B
        tracker.markCompleted(playerId, "node_b").join();
        
        // Now C is unlocked
        assertTrue(validator.isNodeUnlocked(playerId, "node_c").join());
    }
    
    @Test
    void testMultiplePrerequisites() {
        // Setup: A and B both required for C
        TestNode nodeA = new TestNode("node_a", List.of(), List.of("node_c"));
        TestNode nodeB = new TestNode("node_b", List.of(), List.of("node_c"));
        TestNode nodeC = new TestNode("node_c", List.of("node_a", "node_b"), List.of());
        
        validator = new ProgressionValidator<>(tracker, List.of(nodeA, nodeB, nodeC));
        
        // C is locked initially
        assertFalse(validator.isNodeUnlocked(playerId, "node_c").join());
        
        // Complete A only
        tracker.markCompleted(playerId, "node_a").join();
        assertFalse(validator.isNodeUnlocked(playerId, "node_c").join());
        
        // Complete B as well
        tracker.markCompleted(playerId, "node_b").join();
        assertTrue(validator.isNodeUnlocked(playerId, "node_c").join());
    }
    
    @Test
    void testProgressionState() {
        TestNode nodeA = new TestNode("node_a", List.of(), List.of("node_b"));
        TestNode nodeB = new TestNode("node_b", List.of("node_a"), List.of());
        
        validator = new ProgressionValidator<>(tracker, List.of(nodeA, nodeB));
        
        // Node A is available (initial node)
        ProgressionState<TestNode> stateA = validator.getProgressionState(playerId, "node_a").join();
        assertEquals(ProgressionStatus.AVAILABLE, stateA.status());
        assertTrue(stateA.missingPrerequisites().isEmpty());
        
        // Node B is locked
        ProgressionState<TestNode> stateB = validator.getProgressionState(playerId, "node_b").join();
        assertEquals(ProgressionStatus.LOCKED, stateB.status());
        assertEquals(List.of("node_a"), stateB.missingPrerequisites());
        
        // Mark A as active
        tracker.markActive(playerId, "node_a");
        stateA = validator.getProgressionState(playerId, "node_a").join();
        assertEquals(ProgressionStatus.ACTIVE, stateA.status());
        
        // Complete A
        tracker.markCompleted(playerId, "node_a").join();
        stateA = validator.getProgressionState(playerId, "node_a").join();
        assertEquals(ProgressionStatus.COMPLETED, stateA.status());
        
        // Now B is available
        stateB = validator.getProgressionState(playerId, "node_b").join();
        assertEquals(ProgressionStatus.AVAILABLE, stateB.status());
        assertTrue(stateB.missingPrerequisites().isEmpty());
    }
    
    @Test
    void testProcessCompletion() {
        // Setup: A -> B and A -> C (both depend on A)
        TestNode nodeA = new TestNode("node_a", List.of(), List.of("node_b", "node_c"));
        TestNode nodeB = new TestNode("node_b", List.of("node_a"), List.of());
        TestNode nodeC = new TestNode("node_c", List.of("node_a"), List.of());
        
        validator = new ProgressionValidator<>(tracker, List.of(nodeA, nodeB, nodeC));
        
        // Complete A
        tracker.markCompleted(playerId, "node_a").join();
        
        // Process completion should return both B and C as unlocked
        List<TestNode> unlocked = validator.processCompletion(playerId, "node_a").join();
        assertEquals(2, unlocked.size());
        assertTrue(unlocked.stream().anyMatch(n -> n.getIdentifier().equals("node_b")));
        assertTrue(unlocked.stream().anyMatch(n -> n.getIdentifier().equals("node_c")));
    }
    
    @Test
    void testGetUnlockedNodes() {
        TestNode nodeA = new TestNode("node_a", List.of(), List.of("node_b"));
        TestNode nodeB = new TestNode("node_b", List.of("node_a"), List.of("node_c"));
        TestNode nodeC = new TestNode("node_c", List.of("node_b"), List.of());
        
        validator = new ProgressionValidator<>(tracker, List.of(nodeA, nodeB, nodeC));
        
        // Initially only A is unlocked (AVAILABLE)
        List<TestNode> unlocked = validator.getUnlockedNodes(playerId, List.of(nodeA, nodeB, nodeC)).join();
        assertEquals(1, unlocked.size());
        assertEquals("node_a", unlocked.get(0).getIdentifier());
        
        // Complete A
        tracker.markCompleted(playerId, "node_a").join();
        
        // Now A (COMPLETED) and B (AVAILABLE) are both unlocked
        unlocked = validator.getUnlockedNodes(playerId, List.of(nodeA, nodeB, nodeC)).join();
        assertEquals(2, unlocked.size());
        assertTrue(unlocked.stream().anyMatch(n -> n.getIdentifier().equals("node_a")));
        assertTrue(unlocked.stream().anyMatch(n -> n.getIdentifier().equals("node_b")));
    }
    
    @Test
    void testCircularDependencyDetection() {
        // Setup: A -> B -> C -> A (circular)
        TestNode nodeA = new TestNode("node_a", List.of("node_c"), List.of("node_b"));
        TestNode nodeB = new TestNode("node_b", List.of("node_a"), List.of("node_c"));
        TestNode nodeC = new TestNode("node_c", List.of("node_b"), List.of("node_a"));
        
        validator = new ProgressionValidator<>(tracker, List.of(nodeA, nodeB, nodeC));
        
        // Should throw CircularDependencyException
        assertThrows(CircularDependencyException.class, () -> validator.validatePrerequisiteChains());
    }
    
    @Test
    void testSelfDependency() {
        // Setup: A -> A (self-dependency)
        TestNode nodeA = new TestNode("node_a", List.of("node_a"), List.of());
        
        validator = new ProgressionValidator<>(tracker, List.of(nodeA));
        
        // Should throw CircularDependencyException
        assertThrows(CircularDependencyException.class, () -> validator.validatePrerequisiteChains());
    }
    
    @Test
    void testValidComplexGraph() {
        // Setup: Complex but valid DAG
        //     A
        //    / \
        //   B   C
        //    \ /
        //     D
        TestNode nodeA = new TestNode("node_a", List.of(), List.of("node_b", "node_c"));
        TestNode nodeB = new TestNode("node_b", List.of("node_a"), List.of("node_d"));
        TestNode nodeC = new TestNode("node_c", List.of("node_a"), List.of("node_d"));
        TestNode nodeD = new TestNode("node_d", List.of("node_b", "node_c"), List.of());
        
        validator = new ProgressionValidator<>(tracker, List.of(nodeA, nodeB, nodeC, nodeD));
        
        // Should not throw
        assertDoesNotThrow(() -> validator.validatePrerequisiteChains());
        
        // Test progression
        assertTrue(validator.isNodeUnlocked(playerId, "node_a").join());
        assertFalse(validator.isNodeUnlocked(playerId, "node_d").join());
        
        // Complete A
        tracker.markCompleted(playerId, "node_a").join();
        assertTrue(validator.isNodeUnlocked(playerId, "node_b").join());
        assertTrue(validator.isNodeUnlocked(playerId, "node_c").join());
        assertFalse(validator.isNodeUnlocked(playerId, "node_d").join());
        
        // Complete B and C
        tracker.markCompleted(playerId, "node_b").join();
        tracker.markCompleted(playerId, "node_c").join();
        assertTrue(validator.isNodeUnlocked(playerId, "node_d").join());
    }
    
    @Test
    void testInvalidateCache() {
        TestNode nodeA = new TestNode("node_a", List.of(), List.of());
        validator = new ProgressionValidator<>(tracker, List.of(nodeA));
        
        // Should not throw
        assertDoesNotThrow(() -> validator.invalidatePlayerCache(playerId));
    }
    
    @Test
    void testNonExistentNode() {
        TestNode nodeA = new TestNode("node_a", List.of(), List.of());
        validator = new ProgressionValidator<>(tracker, List.of(nodeA));
        
        // Non-existent node should return false
        assertFalse(validator.isNodeUnlocked(playerId, "non_existent").join());
        
        // Should throw for getProgressionState
        assertThrows(IllegalArgumentException.class, 
            () -> validator.getProgressionState(playerId, "non_existent").join());
    }
}
