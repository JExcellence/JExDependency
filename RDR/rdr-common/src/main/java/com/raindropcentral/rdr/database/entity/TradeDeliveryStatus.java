package com.raindropcentral.rdr.database.entity;

/**
 * Claim status for persisted trade delivery payouts.
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public enum TradeDeliveryStatus {
    /**
     * Delivery is waiting for recipient claim.
     */
    PENDING,
    /**
     * Recipient claimed this delivery.
     */
    CLAIMED
}
