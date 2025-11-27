/**
 * Edition feature management for RDQ Free and Premium editions.
 *
 * <p>Provides feature gating and edition-specific configuration:
 * <ul>
 *   <li>{@link EditionFeatures} - Interface for edition feature checks</li>
 *   <li>{@link FreeEditionFeatures} - Free edition limitations</li>
 *   <li>{@link PremiumEditionFeatures} - Premium edition full features</li>
 *   <li>{@link FeatureGate} - Utility for checking features and sending messages</li>
 * </ul>
 *
 * @since 6.0.0
 */
package com.raindropcentral.rdq.shared.edition;
