package com.raindropcentral.rdq.bounty.type;

/**
 * Defines how bounty rewards are delivered to hunters.
 */
public enum DistributionMode {
    /**
     * Items are added directly to the hunter's inventory.
     * Excess items are dropped if inventory is full.
     */
    INSTANT,
    
    /**
     * Items are credited to the hunter's virtual storage system.
     */
    VIRTUAL,
    
    /**
     * Items are dropped at the target's death location.
     */
    DROP,
    
    /**
     * Items are placed in a chest at the death location.
     */
    CHEST
}
