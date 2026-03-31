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

package com.raindropcentral.rds.configs;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.Locale;

/**
 * Represents the store currency configuration section.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@CSAlways
@SuppressWarnings("unused")
public class StoreCurrencySection extends AConfigSection {
	
	private String type;
	
	private Double initial_cost;
	
	private Double growth_rate;
	
	/**
	 * Creates a new store currency section.
	 *
	 * @param baseEnvironment evaluation environment used for config expressions
	 */
	public StoreCurrencySection(EvaluationEnvironmentBuilder baseEnvironment) {
		super(baseEnvironment);
	}
	
	/**
	 * Returns the type.
	 *
	 * @return the type
	 */
	public String getType() {
		return type == null || type.isBlank()
				? "vault"
				: type.trim().toLowerCase(Locale.ROOT);
	}
	
	/**
	 * Returns the initial cost.
	 *
	 * @return the initial cost
	 */
	public double getInitialCost() {
		return initial_cost == null ? 1000.0 : initial_cost;
	}
	
	/**
	 * Returns the growth rate.
	 *
	 * @return the growth rate
	 */
	public double getGrowthRate() {
		return growth_rate == null ? 1.125 : growth_rate;
	}
	
	/**
	 * Sets context.
	 */
	public void setContext(
		String type,
		double initial_cost,
		double growth_rate
	) {
		this.type = type;
		this.initial_cost = initial_cost;
		this.growth_rate = growth_rate;
	}
}
