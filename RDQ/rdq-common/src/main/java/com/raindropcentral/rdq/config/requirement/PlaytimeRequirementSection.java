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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Configuration section for playtime-based requirements.
 *
 * <p>This section handles all configuration options specific to PlaytimeRequirement,
 * including required playtime in various units, world-specific requirements,
 * and conversion utilities.
 *
 * @author JExcellence
 * @version 1.1.0
 * @since TBD
 */
@CSAlways
public class PlaytimeRequirementSection extends AConfigSection {
	
	/**
	 * Required playtime in seconds.
	 * YAML key: "requiredPlaytimeSeconds"
	 */
	private Long requiredPlaytimeSeconds;
	
	/**
	 * Playtime in minutes (converted to seconds).
	 * YAML key: "requiredPlaytimeMinutes"
	 */
	private Long requiredPlaytimeMinutes;
	
	/**
	 * Playtime in hours (converted to seconds).
	 * YAML key: "requiredPlaytimeHours"
	 */
	private Long requiredPlaytimeHours;
	
	/**
	 * Playtime in days (converted to seconds).
	 * YAML key: "requiredPlaytimeDays"
	 */
	private Long requiredPlaytimeDays;
	
	/**
	 * Alternative time field name.
	 * YAML key: "time"
	 */
	private Long time;
	
	/**
	 * Time unit for the time field (seconds, minutes, hours, days).
	 * YAML key: "timeUnit"
	 */
	private String timeUnit;
	
	/**
	 * Whether to use total playtime across all worlds (true) or world-specific playtime (false).
	 * YAML key: "useTotalPlaytime"
	 */
	private Boolean useTotalPlaytime;
	
	/**
	 * Map of world names to required playtime in seconds for world-specific validation.
	 * YAML key: "worldPlaytimeRequirements"
	 */
	private Map<String, Long> worldPlaytimeRequirements;
	
	/**
	 * List of world names where playtime should be tracked.
	 * YAML key: "worlds"
	 */
	private List<String> worlds;
	
	/**
	 * Required playtime for each world in the worlds list (applies to all worlds equally).
	 * YAML key: "worldPlaytimeSeconds"
	 */
	private Long worldPlaytimeSeconds;
	
	/**
	 * Required playtime for each world in minutes.
	 * YAML key: "worldPlaytimeMinutes"
	 */
	private Long worldPlaytimeMinutes;
	
	/**
	 * Required playtime for each world in hours.
	 * YAML key: "worldPlaytimeHours"
	 */
	private Long worldPlaytimeHours;
	
	/**
	 * Required playtime for each world in days.
	 * YAML key: "worldPlaytimeDays"
	 */
	private Long worldPlaytimeDays;
	
	/**
	 * Optional description for this playtime requirement.
	 * YAML key: "description"
	 */
	private String description;
	
	/**
	 * Constructs a new PlaytimeRequirementSection.
	 *
	 * @param evaluationEnvironmentBuilder the evaluation environment builder
	 */
	public PlaytimeRequirementSection(
		final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder
	) {
		super(evaluationEnvironmentBuilder);
	}
	
	/**
	 * Gets the required playtime in seconds, converting from other units if necessary.
	 *
	 * @return the required playtime in seconds
	 */
	public Long getRequiredPlaytimeSeconds() {
		if (this.requiredPlaytimeSeconds != null) {
			return this.requiredPlaytimeSeconds;
		}
		if (this.requiredPlaytimeMinutes != null) {
			return TimeUnit.MINUTES.toSeconds(this.requiredPlaytimeMinutes);
		}
		if (this.requiredPlaytimeHours != null) {
			return TimeUnit.HOURS.toSeconds(this.requiredPlaytimeHours);
		}
		if (this.requiredPlaytimeDays != null) {
			return TimeUnit.DAYS.toSeconds(this.requiredPlaytimeDays);
		}
		if (this.time != null) {
			return this.convertTimeToSeconds(this.time, getTimeUnit());
		}
		return 0L;
	}
	
	/**
	 * Gets the time unit for the generic time field.
	 *
	 * @return the time unit, defaulting to "seconds"
	 */
	public String getTimeUnit() {
		return this.timeUnit != null ? this.timeUnit.toLowerCase() : "seconds";
	}
	
	/**
	 * Gets whether to use total playtime across all worlds.
	 *
	 * @return true to use total playtime, false for world-specific, null for auto-detection
	 */
	@Nullable
	public Boolean getUseTotalPlaytime() {
		if (this.useTotalPlaytime != null) {
			return this.useTotalPlaytime;
		}
		
		// Auto-detect based on configuration
		if (hasWorldSpecificConfiguration()) {
			return false;
		}
		
		return true; // Default to total playtime
	}
	
	/**
	 * Gets the world-specific playtime requirements map.
	 *
	 * @return map of world names to required playtime in seconds
	 */
	@NotNull
	public Map<String, Long> getWorldPlaytimeRequirements() {
		Map<String, Long> requirements = new HashMap<>();
		
		// Add explicit world requirements
		if (this.worldPlaytimeRequirements != null) {
			requirements.putAll(this.worldPlaytimeRequirements);
		}
		
		// Add requirements from worlds list with uniform playtime
		if (this.worlds != null && !this.worlds.isEmpty()) {
			Long worldPlaytime = getWorldPlaytimeSeconds();
			if (worldPlaytime > 0) {
				for (String world : this.worlds) {
					if (world != null && !world.trim().isEmpty()) {
						requirements.put(world.trim(), worldPlaytime);
					}
				}
			}
		}
		
		return requirements;
	}
	
	/**
	 * Gets the required playtime for individual worlds in seconds.
	 *
	 * @return the required playtime per world in seconds
	 */
	public Long getWorldPlaytimeSeconds() {
		if (this.worldPlaytimeSeconds != null) {
			return this.worldPlaytimeSeconds;
		}
		if (this.worldPlaytimeMinutes != null) {
			return TimeUnit.MINUTES.toSeconds(this.worldPlaytimeMinutes);
		}
		if (this.worldPlaytimeHours != null) {
			return TimeUnit.HOURS.toSeconds(this.worldPlaytimeHours);
		}
		if (this.worldPlaytimeDays != null) {
			return TimeUnit.DAYS.toSeconds(this.worldPlaytimeDays);
		}
		return 0L;
	}
	
	/**
	 * Gets the list of worlds for playtime tracking.
	 *
	 * @return list of world names
	 */
	@Nullable
	public List<String> getWorlds() {
		return this.worlds;
	}
	
	/**
	 * Gets the description for this playtime requirement.
	 *
	 * @return the description, or null if not provided
	 */
	@Nullable
	public String getDescription() {
		return this.description;
	}
	
	/**
	 * Gets the generic time value.
	 *
	 * @return the time value
	 */
	@Nullable
	public Long getTime() {
		return this.time;
	}
	
	/**
	 * Checks if this configuration has world-specific settings.
	 *
	 * @return true if world-specific configuration is present
	 */
	public boolean hasWorldSpecificConfiguration() {
		return (this.worldPlaytimeRequirements != null && !this.worldPlaytimeRequirements.isEmpty()) ||
		       (this.worlds != null && !this.worlds.isEmpty() && getWorldPlaytimeSeconds() > 0);
	}
	
	/**
	 * Validates the configuration for consistency.
	 *
	 * @throws IllegalStateException if the configuration is invalid
	 */
	public void validate() {
		// Check that at least one playtime requirement is specified
		if (getRequiredPlaytimeSeconds() <= 0 && !hasWorldSpecificConfiguration()) {
			throw new IllegalStateException("At least one playtime requirement must be specified");
		}
		
		// Check for conflicting configurations
		if (getUseTotalPlaytime() == Boolean.FALSE && !hasWorldSpecificConfiguration()) {
			throw new IllegalStateException("useTotalPlaytime is false but no world-specific requirements are configured");
		}
		
		// Validate world requirements
		Map<String, Long> worldRequirements = getWorldPlaytimeRequirements();
		for (Map.Entry<String, Long> entry : worldRequirements.entrySet()) {
			if (entry.getKey() == null || entry.getKey().trim().isEmpty()) {
				throw new IllegalStateException("World name cannot be null or empty");
			}
			if (entry.getValue() <= 0) {
				throw new IllegalStateException("World playtime requirement must be positive: " + entry.getValue());
			}
		}
	}
	
	/**
	 * Converts time value to seconds based on the specified unit.
	 *
	 * @param timeValue the time value
	 * @param unit the time unit
	 * @return the time in seconds
	 */
	private Long convertTimeToSeconds(
		final @NotNull Long timeValue,
		final @NotNull String unit
	) {
		return switch (unit.toLowerCase()) {
			case "minutes", "minute", "min", "m" -> TimeUnit.MINUTES.toSeconds(timeValue);
			case "hours", "hour", "hr", "h" -> TimeUnit.HOURS.toSeconds(timeValue);
			case "days", "day", "d" -> TimeUnit.DAYS.toSeconds(timeValue);
			case "weeks", "week", "w" -> TimeUnit.DAYS.toSeconds(timeValue * 7);
			default -> timeValue; // Assume seconds
		};
	}
}
