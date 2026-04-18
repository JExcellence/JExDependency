/**
 * Generic progression system supporting sequential prerequisites, branching paths,
 * and automatic unlocking on completion.
 *
 * <p>Implement {@link de.jexcellence.jexplatform.progression.ProgressionNode} for any
 * entity (quests, ranks, achievements) and use
 * {@link de.jexcellence.jexplatform.progression.ProgressionValidator} or its cached
 * variant to validate prerequisite chains and determine unlock status.
 *
 * @author JExcellence
 * @since 1.0.0
 */
package de.jexcellence.jexplatform.progression;
