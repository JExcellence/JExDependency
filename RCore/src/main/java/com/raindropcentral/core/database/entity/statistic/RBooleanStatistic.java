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

package com.raindropcentral.core.database.entity.statistic;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Represents the type API type.
 */
/**
 * Represents the RBooleanStatistic API type.
 */
@Entity
@DiscriminatorValue("BOOLEAN")
public class RBooleanStatistic extends RAbstractStatistic {
    
    @Column(name = "statistic_boolean")
    private Boolean value;
    
    protected RBooleanStatistic() {}
    
    /**
     * Executes RBooleanStatistic.
     */
    public RBooleanStatistic(
        final @NotNull String identifier,
        final @NotNull String plugin,
        final @NotNull Boolean value
    ) {
        /**
         * Executes super.
         */
        super(identifier, plugin);
        this.value = Objects.requireNonNull(value, "value cannot be null");
    }
    /**
     * Executes this member.
     */
    
    /**
     * Gets value.
     */
    @Override
    public @NotNull Boolean getValue() {
        return this.value;
    }
    
    /**
     * Executes toggle.
     */
    public void toggle() {
        this.value = !this.value;
    }
    
    /**
     * Sets value.
     */
    public void setValue(final @NotNull Boolean value) {
        this.value = Objects.requireNonNull(value, "value cannot be null");
    }
    
    /**
     * Executes toString.
     */
    @Override
    public String toString() {
        return "RBooleanStatistic[id=%d, identifier=%s, plugin=%s, value=%b]"
            .formatted(getId(), identifier, getPlugin(), value);
    }
}
