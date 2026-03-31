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

package com.raindropcentral.rdq.config.requirement;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration section for time-based requirements.
 *
 * <p>This section handles all configuration options specific to TimeBasedRequirement,
 * including time constraints, cooldowns, scheduling, and time zone handling.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
@CSAlways
public class TimeBasedRequirementSection extends AConfigSection {
	
	/**
	 * Time constraint in seconds.
	 * YAML key: "timeConstraintSeconds"
	 */
	private Long timeConstraintSeconds;
	
	/**
	 * Cooldown period in seconds.
	 * YAML key: "cooldownSeconds"
	 */
	private Long cooldownSeconds;
	
	/**
	 * Start time for time-based availability (HH:mm format).
	 * YAML key: "startTime"
	 */
	private String startTime;
	
	/**
	 * End time for time-based availability (HH:mm format).
	 * YAML key: "endTime"
	 */
	private String endTime;
	
	/**
	 * Time zone for time calculations.
	 * YAML key: "timeZone"
	 */
	private String timeZone;
	
	/**
	 * Whether this requirement is recurring (resets after completion).
	 * YAML key: "recurring"
	 */
	private Boolean recurring;
	
	/**
	 * Days of the week when this requirement is active (1=Monday, 7=Sunday).
	 * YAML key: "activeDays"
	 */
	private List<Integer> activeDays;
	
	/**
	 * Specific dates when this requirement is active (yyyy-MM-dd format).
	 * YAML key: "activeDates"
	 */
	private List<String> activeDates;
	
	/**
	 * Whether to check real-world time or server uptime.
	 * YAML key: "useRealTime"
	 */
	private Boolean useRealTime;
	
	/**
	 * Constructs a new TimeBasedRequirementSection.
	 *
	 * @param evaluationEnvironmentBuilder the evaluation environment builder
	 */
	public TimeBasedRequirementSection(
		final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder
	) {
		
		super(evaluationEnvironmentBuilder);
	}
	
	/**
	 * Gets the time constraint in seconds, trying multiple field names.
	 *
	 * @return the time constraint in seconds
	 */
	public Long getTimeConstraintSeconds() {
		
		if (
			this.timeConstraintSeconds != null
		) {
			return this.timeConstraintSeconds;
		}
		return 0L;
	}
	
	/**
	 * Gets the cooldown in seconds, trying multiple field names.
	 *
	 * @return the cooldown in seconds
	 */
	public Long getCooldownSeconds() {
		
		if (
			this.cooldownSeconds != null
		) {
			return this.cooldownSeconds;
		}
		return 0L;
	}
	
	/**
	 * Gets the start time for availability.
	 *
	 * @return the start time in HH:mm format
	 */
	public String getStartTime() {
		
		return this.startTime;
	}
	
	/**
	 * Gets the end time for availability.
	 *
	 * @return the end time in HH:mm format
	 */
	public String getEndTime() {
		
		return this.endTime;
	}
	
	/**
	 * Gets the time zone for time calculations.
	 *
	 * @return the time zone identifier, defaulting to "UTC"
	 */
	public String getTimeZone() {
		
		return this.timeZone != null ?
		       this.timeZone :
		       "UTC";
	}
	
	/**
	 * Gets whether this requirement is recurring.
	 *
	 * @return true if the requirement resets after completion
	 */
	public Boolean getRecurring() {
		
		return this.recurring != null ?
		       this.recurring :
		       false;
	}
	
	/**
	 * Gets the active days of the week.
	 *
	 * @return list of active days (1=Monday, 7=Sunday)
	 */
	public List<Integer> getActiveDays() {
		
		return this.activeDays != null ?
		       this.activeDays :
		       new ArrayList<>();
	}
	
	/**
	 * Gets the specific active dates.
	 *
	 * @return list of active dates in yyyy-MM-dd format
	 */
	public List<String> getActiveDates() {
		
		return this.activeDates != null ?
		       this.activeDates :
		       new ArrayList<>();
	}
	
	/**
	 * Gets whether to use real-world time or server uptime.
	 *
	 * @return true to use real-world time, false for server uptime
	 */
	public Boolean getUseRealTime() {
		
		return this.useRealTime != null ?
		       this.useRealTime :
		       true;
	}
	
}
