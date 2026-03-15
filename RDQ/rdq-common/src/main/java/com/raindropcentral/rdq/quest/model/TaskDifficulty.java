package com.raindropcentral.rdq.quest.model;

/**
 * Enum representing the difficulty levels for individual quest tasks.
 *
 * <p>Task difficulty is independent of quest difficulty and allows for
 * fine-grained control over individual task challenges within a quest.
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public enum TaskDifficulty {
    
    /**
     * Trivial difficulty - very simple tasks.
     */
    TRIVIAL,
    
    /**
     * Easy difficulty - simple tasks requiring minimal effort.
     */
    EASY,
    
    /**
     * Medium difficulty - standard tasks requiring moderate effort.
     */
    MEDIUM,
    
    /**
     * Hard difficulty - challenging tasks requiring significant effort.
     */
    HARD,
    
    /**
     * Extreme difficulty - most challenging tasks requiring maximum effort.
     */
    EXTREME
}
