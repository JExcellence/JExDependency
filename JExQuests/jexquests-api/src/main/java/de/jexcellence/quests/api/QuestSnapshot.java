package de.jexcellence.quests.api;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Immutable snapshot of a quest definition. Safe to pass across plugin
 * boundaries without exposing the underlying entity.
 *
 * @param identifier stable identifier
 * @param category category key
 * @param displayName MiniMessage-formatted display name
 * @param difficulty difficulty level name
 * @param repeatable whether the quest may be completed more than once
 * @param maxCompletions cap on repeat completions (0 = unlimited)
 * @param cooldownSeconds minimum seconds between completions
 * @param timeLimitSeconds quest time limit once accepted (0 = untimed)
 * @param enabled whether the quest is currently available
 * @param taskIdentifiers ordered list of task identifiers
 */
public record QuestSnapshot(
        @NotNull String identifier,
        @NotNull String category,
        @NotNull String displayName,
        @NotNull String difficulty,
        boolean repeatable,
        int maxCompletions,
        long cooldownSeconds,
        long timeLimitSeconds,
        boolean enabled,
        @NotNull List<String> taskIdentifiers
) {
    public QuestSnapshot {
        taskIdentifiers = List.copyOf(taskIdentifiers);
    }
}
