package com.raindropcentral.rdq.bounty.type;

/**
 * Defines the sort order for hunter leaderboards.
 */
public enum HunterSortOrder {
    /**
     * Sort by total number of bounties claimed (descending).
     */
    BOUNTIES_CLAIMED,
    
    /**
     * Sort by total reward value accumulated (descending).
     */
    TOTAL_REWARD_VALUE,
    
    /**
     * Sort by highest single bounty value (descending).
     */
    HIGHEST_BOUNTY_VALUE,
    
    /**
     * Sort by most recent claim timestamp (descending).
     */
    RECENT_CLAIMS
}
