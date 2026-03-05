package com.raindropcentral.rdt.service;

/**
 * Defines edition-specific runtime behavior for RDT.
 *
 * <p>The shared runtime delegates configuration mutability checks and edition metadata to this
 * abstraction so free and premium jars can share one codebase.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public interface TownService {

    /**
     * Returns whether the active edition allows runtime config changes.
     *
     * @return {@code true} when config changes are allowed
     */
    boolean canChangeConfigs();

    /**
     * Returns whether the active edition is premium.
     *
     * @return {@code true} for premium editions
     */
    boolean isPremium();
}
