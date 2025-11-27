/**
 * Service layer for rank system business logic.
 *
 * <p>Contains service implementations:
 * <ul>
 *   <li>{@link com.raindropcentral.rdq.rank.service.DefaultFreeRankService} - Free edition rank service with single active tree</li>
 *   <li>{@link com.raindropcentral.rdq.rank.service.DefaultPremiumRankService} - Premium edition with multiple trees and cross-tree switching</li>
 *   <li>{@link com.raindropcentral.rdq.rank.service.RankRequirementChecker} - Pattern matching requirement validation</li>
 * </ul>
 *
 * @since 6.0.0
 */
package com.raindropcentral.rdq.rank.service;
