package de.jexcellence.quests.database.entity;

/**
 * Lifecycle state of a player's attempt at a specific quest.
 */
public enum QuestStatus {
    AVAILABLE,
    ACTIVE,
    COMPLETED,
    FAILED,
    EXPIRED,
    ABANDONED
}
