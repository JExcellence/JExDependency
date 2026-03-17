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
 * Represents the RNumberStatistic API type.
 */
@Entity
@DiscriminatorValue("NUMBER")
public class RNumberStatistic extends RAbstractStatistic {
    
    @Column(name = "statistic_number")
    private Double value;
    
    protected RNumberStatistic() {}
    
    public RNumberStatistic(
        final @NotNull String identifier,
        final @NotNull String plugin,
        final @NotNull Double value
    ) {
        super(identifier, plugin);
        this.value = Objects.requireNonNull(value, "value cannot be null");
    }
    
    @Override
    public @NotNull Double getValue() {
        return this.value;
    }

    public void setValue(final Double value) { this.value = value; }

    /**
     * Performs increment.
     */
    public void increment(final double amount) {
        this.value += amount;
    }
    
    /**
     * Performs decrement.
     */
    public void decrement(final double amount) {
        this.value = Math.max(0.0, this.value - amount);
    }
    
    /**
     * Performs multiply.
     */
    public void multiply(final double factor) {
        this.value *= factor;
    }
    
    /**
     * Returns whether positive.
     */
    public boolean isPositive() {
        return this.value > 0.0;
    }
    
    /**
     * Returns whether zero.
     */
    public boolean isZero() {
        return Math.abs(this.value) < 0.0001;
    }
    
    @Override
    public String toString() {
        return "RNumberStatistic[id=%d, identifier=%s, plugin=%s, value=%.2f]"
            .formatted(getId(), identifier, getPlugin(), value);
    }
}
