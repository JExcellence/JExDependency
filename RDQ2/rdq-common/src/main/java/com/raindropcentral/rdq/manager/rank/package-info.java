/**
 * Contracts for rank management across RDQ editions.
 * <p>
 * {@link com.raindropcentral.rdq.manager.rank.RankManager} is intentionally lean so individual
 * editions can layer their own storage, validation, and synchronization policies while retaining the
 * shared lifecycle and navigation guarantees enforced by the parent manager.
 * </p>
 */
package com.raindropcentral.rdq.manager.rank;
