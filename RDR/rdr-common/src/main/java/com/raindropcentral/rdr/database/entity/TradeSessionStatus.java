package com.raindropcentral.rdr.database.entity;

/**
 * Lifecycle states for a persisted trade session.
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public enum TradeSessionStatus {
    /**
     * Trade invite exists and awaits partner acceptance.
     */
    INVITED,
    /**
     * Trade is active and participants may edit offers.
     */
    ACTIVE,
    /**
     * Offer editing is locked and participants are confirming settlement.
     */
    COMPLETING,
    /**
     * Settlement was finalized and delivery rows were created.
     */
    COMPLETED,
    /**
     * Session was canceled by a participant.
     */
    CANCELED,
    /**
     * Invite/session expired before settlement.
     */
    EXPIRED;

    /**
     * Returns whether this status is terminal.
     *
     * @return {@code true} when the session should no longer mutate
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELED || this == EXPIRED;
    }
}
