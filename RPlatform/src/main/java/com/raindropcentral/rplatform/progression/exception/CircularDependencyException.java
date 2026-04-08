package com.raindropcentral.rplatform.progression.exception;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Exception thrown when a circular dependency is detected in a progression chain.
 * <p>
 * This exception indicates that a node's prerequisite chain forms a cycle,
 * which would make it impossible to complete the progression. For example:
 * Quest A requires Quest B, Quest B requires Quest C, and Quest C requires Quest A.
 *
 * <h2>Common Scenarios:</h2>
 * <ul>
 *     <li>Direct cycle: A → B → A</li>
 *     <li>Indirect cycle: A → B → C → A</li>
 *     <li>Self-dependency: A → A</li>
 *     <li>Complex cycle: A → B → C → D → B</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * try {
 *     validator.validatePrerequisiteChains();
 * } catch (CircularDependencyException e) {
 *     logger.severe("Invalid progression configuration: " + e.getMessage());
 *     // Handle configuration error
 * }
 * }</pre>
 *
 * <h2>Prevention:</h2>
 * <p>
 * To prevent circular dependencies:
 * <ul>
 *     <li>Validate progression configuration on plugin startup</li>
 *     <li>Use {@link com.raindropcentral.rplatform.progression.ProgressionValidator#validatePrerequisiteChains()}</li>
 *     <li>Design progression as a directed acyclic graph (DAG)</li>
 *     <li>Avoid bidirectional dependencies between nodes</li>
 *     <li>Test configuration changes before deployment</li>
 * </ul>
 *
 * <h2>Detection Algorithm:</h2>
 * <p>
 * This exception is thrown by the progression validator when it detects a cycle
 * using depth-first search with a recursion stack. The algorithm:
 * <ol>
 *     <li>Traverses each node's prerequisite chain</li>
 *     <li>Maintains a recursion stack of currently visiting nodes</li>
 *     <li>If a node is encountered that's already in the stack, a cycle exists</li>
 *     <li>Throws this exception with details about the cycle</li>
 * </ol>
 *
 * <h2>Error Message Format:</h2>
 * <p>
 * The exception message typically includes:
 * <ul>
 *     <li>The node identifier where the cycle was detected</li>
 *     <li>The chain of nodes forming the cycle (if available)</li>
 *     <li>Suggestions for fixing the configuration</li>
 * </ul>
 *
 * @author RaindropCentral
 * @version 1.0.0
 * @since 1.0.0
 */
public class CircularDependencyException extends RuntimeException {
    
    /**
     * Constructs a new circular dependency exception with no detail message.
     */
    public CircularDependencyException() {
        super();
    }
    
    /**
     * Constructs a new circular dependency exception with the specified detail message.
     * <p>
     * The message should clearly identify the nodes involved in the cycle
     * to help administrators fix the configuration.
     *
     * @param message The detail message explaining the circular dependency
     */
    public CircularDependencyException(@NotNull String message) {
        super(message);
    }
    
    /**
     * Constructs a new circular dependency exception with the specified detail message and cause.
     * <p>
     * This constructor is useful when the circular dependency detection
     * fails due to an underlying issue (e.g., database error while loading nodes).
     *
     * @param message The detail message explaining the circular dependency
     * @param cause The cause of the exception (can be null)
     */
    public CircularDependencyException(@NotNull String message, @Nullable Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Constructs a new circular dependency exception with the specified cause.
     * <p>
     * The detail message is set to {@code (cause==null ? null : cause.toString())}.
     *
     * @param cause The cause of the exception (can be null)
     */
    public CircularDependencyException(@Nullable Throwable cause) {
        super(cause);
    }
}
