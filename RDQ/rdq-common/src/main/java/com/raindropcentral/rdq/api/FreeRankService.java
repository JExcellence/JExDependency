package com.raindropcentral.rdq.api;

/**
 * Free edition rank service with limited features.
 *
 * <p>Provides rank progression with a single active rank tree.
 * Cross-tree switching is not available in the free edition.
 *
 * @see RankService
 * @see PremiumRankService
 */
public non-sealed interface FreeRankService extends RankService {
}
