package com.raindropcentral.rplatform.progression;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Interface for any entity that can be part of a sequential progression system.
 * <p>
 * Implementations include Quests, Ranks, Achievements, Story Chapters, and any other
 * feature that requires sequential progression with prerequisites.
 * </p>
 *
 * <p>
 * This interface provides a generic foundation for progression systems, allowing
 * different features to share common progression logic including:
 * </p>
 * <ul>
 *     <li>Prerequisite validation</li>
 *     <li>Sequential progression enforcement</li>
 *     <li>Automatic unlocking when prerequisites are met</li>
 *     <li>Circular dependency detection</li>
 * </ul>
 *
 * <h2>Basic Usage Example:</h2>
 * <pre>{@code
 * @Entity
 * @Table(name = "quests")
 * public class Quest implements IProgressionNode<Quest> {
 *     @Id
 *     private String identifier;
 *     
 *     @ElementCollection
 *     @CollectionTable(name = "quest_prerequisites")
 *     private List<String> previousQuestIds = new ArrayList<>();
 *     
 *     @ElementCollection
 *     @CollectionTable(name = "quest_dependents")
 *     private List<String> nextQuestIds = new ArrayList<>();
 *
 *     @Override
 *     public String getIdentifier() {
 *         return identifier;
 *     }
 *
 *     @Override
 *     public List<String> getPreviousNodeIdentifiers() {
 *         return previousQuestIds;
 *     }
 *
 *     @Override
 *     public List<String> getNextNodeIdentifiers() {
 *         return nextQuestIds;
 *     }
 * }
 * }</pre>
 *
 * <h2>Linear Progression Example:</h2>
 * <p>
 * Creating a simple linear quest chain (Quest A → Quest B → Quest C):
 * </p>
 * <pre>{@code
 * // Quest A (initial node)
 * Quest questA = new Quest("quest_a");
 * questA.setNextQuestIds(List.of("quest_b"));
 * // previousQuestIds is empty - this is an initial node
 *
 * // Quest B (middle node)
 * Quest questB = new Quest("quest_b");
 * questB.setPreviousQuestIds(List.of("quest_a"));
 * questB.setNextQuestIds(List.of("quest_c"));
 *
 * // Quest C (final node)
 * Quest questC = new Quest("quest_c");
 * questC.setPreviousQuestIds(List.of("quest_b"));
 * // nextQuestIds is empty - this is a final node
 * }</pre>
 *
 * <h2>Multiple Prerequisites Example:</h2>
 * <p>
 * Creating a quest that requires multiple prerequisites (Quest A AND Quest B → Quest C):
 * </p>
 * <pre>{@code
 * // Quest A (initial node)
 * Quest questA = new Quest("quest_a");
 * questA.setNextQuestIds(List.of("quest_c"));
 *
 * // Quest B (initial node)
 * Quest questB = new Quest("quest_b");
 * questB.setNextQuestIds(List.of("quest_c"));
 *
 * // Quest C (requires both A and B)
 * Quest questC = new Quest("quest_c");
 * questC.setPreviousQuestIds(List.of("quest_a", "quest_b"));
 * }</pre>
 *
 * <h2>Branching Progression Example:</h2>
 * <p>
 * Creating a quest that unlocks multiple paths (Quest A → Quest B OR Quest C):
 * </p>
 * <pre>{@code
 * // Quest A (initial node that branches)
 * Quest questA = new Quest("quest_a");
 * questA.setNextQuestIds(List.of("quest_b", "quest_c"));
 *
 * // Quest B (one branch)
 * Quest questB = new Quest("quest_b");
 * questB.setPreviousQuestIds(List.of("quest_a"));
 *
 * // Quest C (another branch)
 * Quest questC = new Quest("quest_c");
 * questC.setPreviousQuestIds(List.of("quest_a"));
 * }</pre>
 *
 * <h2>Integration with Progression System:</h2>
 * <p>
 * Once the progression system components are implemented, you can use them as follows:
 * </p>
 * <pre>{@code
 * // Create completion tracker (tracks which nodes players have completed)
 * QuestCompletionTracker tracker = new QuestCompletionTracker(repository);
 *
 * // Create validator with all quests
 * ProgressionValidator validator = new ProgressionValidator(
 *     tracker,
 *     questRepository.findAll().join()
 * );
 *
 * // Validate prerequisite chains on startup to detect circular dependencies
 * try {
 *     validator.validatePrerequisiteChains();
 * } catch (CircularDependencyException e) {
 *     logger.severe("Circular dependency detected: " + e.getMessage());
 * }
 *
 * // Check if a quest is unlocked for a player
 * UUID playerId = player.getUniqueId();
 * validator.isNodeUnlocked(playerId, "quest_b").thenAccept(unlocked -> {
 *     if (unlocked) {
 *         // Player can start quest_b
 *     } else {
 *         // Show locked message with missing prerequisites
 *     }
 * });
 *
 * // Get progression state with missing prerequisites
 * validator.getProgressionState(playerId, "quest_c").thenAccept(state -> {
 *     switch (state.status()) {
 *         case LOCKED -> {
 *             List<String> missing = state.missingPrerequisites();
 *             // Show "Complete quest_a and quest_b first"
 *         }
 *         case AVAILABLE -> {
 *             // Show "Click to start"
 *         }
 *         case ACTIVE -> {
 *             // Show "In progress"
 *         }
 *         case COMPLETED -> {
 *             // Show "Completed"
 *         }
 *     }
 * });
 *
 * // Process completion and get newly unlocked quests
 * validator.processCompletion(playerId, "quest_a").thenAccept(unlocked -> {
 *     for (Quest quest : unlocked) {
 *         // Notify player that quest is now available
 *         player.sendMessage("New quest unlocked: " + quest.getName());
 *     }
 * });
 * }</pre>
 *
 * <h2>Rank System Example:</h2>
 * <pre>{@code
 * @Entity
 * @Table(name = "ranks")
 * public class Rank implements IProgressionNode<Rank> {
 *     @Id
 *     private String identifier;
 *     
 *     @ElementCollection
 *     private List<String> previousRanks = new ArrayList<>();
 *     
 *     @ElementCollection
 *     private List<String> nextRanks = new ArrayList<>();
 *     
 *     private int requiredLevel;
 *     private int requiredCoins;
 *
 *     @Override
 *     public String getIdentifier() {
 *         return identifier;
 *     }
 *
 *     @Override
 *     public List<String> getPreviousNodeIdentifiers() {
 *         return previousRanks;
 *     }
 *
 *     @Override
 *     public List<String> getNextNodeIdentifiers() {
 *         return nextRanks;
 *     }
 * }
 *
 * // Usage with rank progression
 * Rank novice = new Rank("novice");
 * novice.setNextRanks(List.of("apprentice"));
 *
 * Rank apprentice = new Rank("apprentice");
 * apprentice.setPreviousRanks(List.of("novice"));
 * apprentice.setNextRanks(List.of("expert"));
 *
 * Rank expert = new Rank("expert");
 * expert.setPreviousRanks(List.of("apprentice"));
 * }</pre>
 *
 * <h2>Achievement System Example:</h2>
 * <pre>{@code
 * @Entity
 * @Table(name = "achievements")
 * public class Achievement implements IProgressionNode<Achievement> {
 *     @Id
 *     private String identifier;
 *     
 *     @ElementCollection
 *     private List<String> prerequisiteAchievements = new ArrayList<>();
 *     
 *     @ElementCollection
 *     private List<String> dependentAchievements = new ArrayList<>();
 *
 *     @Override
 *     public String getIdentifier() {
 *         return identifier;
 *     }
 *
 *     @Override
 *     public List<String> getPreviousNodeIdentifiers() {
 *         return prerequisiteAchievements;
 *     }
 *
 *     @Override
 *     public List<String> getNextNodeIdentifiers() {
 *         return dependentAchievements;
 *     }
 * }
 * }</pre>
 *
 * <h2>Best Practices:</h2>
 * <ul>
 *     <li>Always return non-null lists from getPreviousNodeIdentifiers() and getNextNodeIdentifiers()</li>
 *     <li>Use empty lists for initial nodes (no prerequisites) and final nodes (no dependents)</li>
 *     <li>Ensure identifiers are unique within your progression system</li>
 *     <li>Validate prerequisite chains on application startup to detect circular dependencies</li>
 *     <li>Use bidirectional relationships: if A lists B in nextNodes, B should list A in previousNodes</li>
 *     <li>Consider using ElementCollection annotation for JPA entities to persist prerequisite lists</li>
 *     <li>Implement proper equals/hashCode based on identifier for use in collections</li>
 * </ul>
 *
 * <h2>Thread Safety:</h2>
 * <p>
 * Implementations should be immutable or thread-safe if used in concurrent contexts.
 * The progression validation system uses thread-safe data structures internally, but node
 * implementations should ensure their identifier and prerequisite lists don't change during validation.
 * </p>
 *
 * @param <T> The concrete type implementing this interface (enables type-safe operations)
 * @author RaindropCentral
 * @version 1.0.0
 * @since 1.0.0
 */
public interface IProgressionNode<T extends IProgressionNode<T>> {
    
    /**
     * Gets the unique identifier for this progression node.
     * <p>
     * This identifier must be unique within the progression system and is used
     * to reference this node in prerequisite relationships.
     * </p>
     *
     * @return unique identifier (never null or empty)
     */
    @NotNull String getIdentifier();
    
    /**
     * Gets the identifiers of nodes that must be completed before this node becomes available.
     * <p>
     * All nodes in this list must be completed before this node can be started.
     * This implements AND logic for prerequisites (all must be completed).
     * </p>
     *
     * <p>
     * For initial nodes (nodes with no prerequisites), return an empty list.
     * </p>
     *
     * @return list of prerequisite node identifiers (never null, may be empty)
     */
    @NotNull List<String> getPreviousNodeIdentifiers();
    
    /**
     * Gets the identifiers of nodes that depend on this node as a prerequisite.
     * <p>
     * When this node is completed, the progression system will check these dependent
     * nodes to see if they can be automatically unlocked.
     * </p>
     *
     * <p>
     * For final nodes (nodes with no dependents), return an empty list.
     * </p>
     *
     * @return list of dependent node identifiers (never null, may be empty)
     */
    @NotNull List<String> getNextNodeIdentifiers();
    
    /**
     * Checks if this is an initial node (no prerequisites).
     * <p>
     * Initial nodes are immediately available to all players and serve as
     * entry points into the progression system.
     * </p>
     *
     * @return true if this node has no prerequisites, false otherwise
     */
    default boolean isInitialNode() {
        return getPreviousNodeIdentifiers().isEmpty();
    }
    
    /**
     * Checks if this is a final node (no dependents).
     * <p>
     * Final nodes represent the end of a progression chain and have no
     * nodes that depend on them.
     * </p>
     *
     * @return true if this node has no dependent nodes, false otherwise
     */
    default boolean isFinalNode() {
        return getNextNodeIdentifiers().isEmpty();
    }
    
    /**
     * Checks if this node has any prerequisites.
     * <p>
     * This is a convenience method equivalent to {@code !isInitialNode()}.
     * </p>
     *
     * @return true if prerequisites exist, false otherwise
     */
    default boolean hasPrerequisites() {
        return !getPreviousNodeIdentifiers().isEmpty();
    }
}
