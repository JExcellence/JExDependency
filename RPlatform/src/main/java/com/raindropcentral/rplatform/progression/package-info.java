/**
 * Provides a generic, reusable progression system for implementing sequential progression
 * with prerequisites across different features.
 * <p>
 * This package contains interfaces and services for managing progression nodes (Quests, Ranks,
 * Achievements, etc.) with support for:
 * <ul>
 *     <li>Prerequisite validation</li>
 *     <li>Sequential progression enforcement</li>
 *     <li>Automatic unlocking when prerequisites are met</li>
 *     <li>Circular dependency detection</li>
 *     <li>Performance-optimized caching</li>
 * </ul>
 *
 * <h2>Core Components:</h2>
 * <ul>
 *     <li>{@link com.raindropcentral.rplatform.progression.IProgressionNode} - Interface for progression nodes</li>
 *     <li>ICompletionTracker - Interface for tracking completion (to be implemented)</li>
 *     <li>ProgressionValidator - Service for validation (to be implemented)</li>
 *     <li>{@link com.raindropcentral.rplatform.progression.model.ProgressionState} - State model</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // 1. Implement IProgressionNode
 * public class Quest implements IProgressionNode<Quest> {
 *     // ... implementation
 * }
 *
 * // 2. Implement ICompletionTracker (once available)
 * public class QuestCompletionTracker implements ICompletionTracker<Quest> {
 *     // ... implementation
 * }
 *
 * // 3. Create ProgressionValidator (once available)
 * ProgressionValidator validator = new ProgressionValidator(
 *     completionTracker,
 *     allQuests
 * );
 *
 * // 4. Validate prerequisites
 * validator.validatePrerequisiteChains(); // On startup
 *
 * // 5. Check if unlocked
 * boolean unlocked = validator.isNodeUnlocked(playerId, questId).join();
 * }</pre>
 *
 * @author RaindropCentral
 * @version 1.0.0
 * @since 1.0.0
 */
package com.raindropcentral.rplatform.progression;
