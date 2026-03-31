package com.raindropcentral.rdq.model.quest;

/**
 * Represents the various states a quest can be in for a player.
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public enum QuestState {
    
    /**
     * Quest requirements are not met - player cannot start.
     */
    LOCKED,
    
    /**
     * Quest is available to start - all requirements met.
     */
    AVAILABLE,
    
    /**
     * Quest is currently in progress.
     */
    ACTIVE,
    
    /**
     * Quest was just completed (this session).
     */
    COMPLETED,
    
    /**
     * Quest is on cooldown - waiting to restart.
     */
    ON_COOLDOWN,
    
    /**
     * Quest cooldown expired - available to restart.
     */
    AVAILABLE_TO_RESTART,
    
    /**
     * Quest has reached maximum completions.
     */
    MAX_COMPLETIONS,
    
    /**
     * Quest is finished (non-repeatable, completed).
     */
    FINISHED
}
