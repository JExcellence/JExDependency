package com.raindropcentral.rplatform.progression;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link IProgressionNode} interface default methods.
 *
 * @author RaindropCentral
 * @version 1.0.0
 * @since 1.0.0
 */
class IProgressionNodeTest {
    
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
    
    @Test
    void testIsInitialNode_WithNoPrerequisites_ReturnsTrue() {
        // Given: A node with no prerequisites
        TestNode node = new TestNode("node1", List.of(), List.of("node2"));
        
        // When & Then: Should be identified as initial node
        assertTrue(node.isInitialNode());
    }
    
    @Test
    void testIsInitialNode_WithPrerequisites_ReturnsFalse() {
        // Given: A node with prerequisites
        TestNode node = new TestNode("node2", List.of("node1"), List.of("node3"));
        
        // When & Then: Should not be identified as initial node
        assertFalse(node.isInitialNode());
    }
    
    @Test
    void testIsFinalNode_WithNoDependents_ReturnsTrue() {
        // Given: A node with no dependent nodes
        TestNode node = new TestNode("node3", List.of("node2"), List.of());
        
        // When & Then: Should be identified as final node
        assertTrue(node.isFinalNode());
    }
    
    @Test
    void testIsFinalNode_WithDependents_ReturnsFalse() {
        // Given: A node with dependent nodes
        TestNode node = new TestNode("node2", List.of("node1"), List.of("node3"));
        
        // When & Then: Should not be identified as final node
        assertFalse(node.isFinalNode());
    }
    
    @Test
    void testHasPrerequisites_WithPrerequisites_ReturnsTrue() {
        // Given: A node with prerequisites
        TestNode node = new TestNode("node2", List.of("node1"), List.of("node3"));
        
        // When & Then: Should have prerequisites
        assertTrue(node.hasPrerequisites());
    }
    
    @Test
    void testHasPrerequisites_WithoutPrerequisites_ReturnsFalse() {
        // Given: A node without prerequisites
        TestNode node = new TestNode("node1", List.of(), List.of("node2"));
        
        // When & Then: Should not have prerequisites
        assertFalse(node.hasPrerequisites());
    }
    
    @Test
    void testInitialAndFinalNode_Standalone_BothTrue() {
        // Given: A standalone node with no prerequisites or dependents
        TestNode node = new TestNode("standalone", List.of(), List.of());
        
        // When & Then: Should be both initial and final
        assertTrue(node.isInitialNode());
        assertTrue(node.isFinalNode());
        assertFalse(node.hasPrerequisites());
    }
    
    @Test
    void testMiddleNode_NotInitialNotFinal() {
        // Given: A middle node in a chain
        TestNode node = new TestNode("middle", List.of("prev"), List.of("next"));
        
        // When & Then: Should be neither initial nor final
        assertFalse(node.isInitialNode());
        assertFalse(node.isFinalNode());
        assertTrue(node.hasPrerequisites());
    }
    
    @Test
    void testMultiplePrerequisites() {
        // Given: A node with multiple prerequisites
        TestNode node = new TestNode("node", List.of("prereq1", "prereq2", "prereq3"), List.of());
        
        // When & Then: Should correctly identify prerequisites
        assertFalse(node.isInitialNode());
        assertTrue(node.hasPrerequisites());
        assertEquals(3, node.getPreviousNodeIdentifiers().size());
    }
    
    @Test
    void testMultipleDependents() {
        // Given: A node with multiple dependents
        TestNode node = new TestNode("node", List.of(), List.of("dep1", "dep2", "dep3"));
        
        // When & Then: Should correctly identify dependents
        assertTrue(node.isInitialNode());
        assertFalse(node.isFinalNode());
        assertEquals(3, node.getNextNodeIdentifiers().size());
    }
}
