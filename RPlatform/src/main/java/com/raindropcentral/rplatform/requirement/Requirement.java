/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rplatform.requirement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Sealed interface for all requirement types in the RPlatform system.
 *
 * <p>Each requirement defines its own type through its class and registration.
 */
public sealed interface Requirement 
    permits AbstractRequirement {

    /**
     * Executes this member.
     */
    /**
     * Returns whether met.
     */
    @NotNull String getTypeId();
    /**
     * Executes isMet.
     */
    boolean isMet(@NotNull Player player);
    /**
     * Executes calculateProgress.
     */
    double calculateProgress(@NotNull Player player);
    /**
     * Executes consume.
     */
    void consume(@NotNull Player player);
    
    /**
     * Executes this member.
     */
    @JsonIgnore
    @NotNull String getDescriptionKey();
}
