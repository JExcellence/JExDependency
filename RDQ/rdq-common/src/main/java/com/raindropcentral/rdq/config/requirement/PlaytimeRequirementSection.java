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
 * <p>
 * This section handles all configuration options specific to {@code PlaytimeRequirement},
 * including required playtime in various units, world-specific requirements, and
 * conversion utilities that normalize each configuration input to seconds for downstream
 * validation.
 * </p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@CSAlways
public class PlaytimeRequirementSection extends AConfigSection {
	
        /**
         * Required playtime in seconds as declared in configuration.
         * YAML key: {@code requiredPlaytimeSeconds}.
         */
        private Long requiredPlaytimeSeconds;

        /**
         * Required playtime in minutes, converted to seconds for evaluation.
         * YAML key: {@code requiredPlaytimeMinutes}.
         */
        private Long requiredPlaytimeMinutes;

        /**
         * Required playtime in hours, converted to seconds for evaluation.
         * YAML key: {@code requiredPlaytimeHours}.
         */
        private Long requiredPlaytimeHours;

        /**
         * Required playtime in days, converted to seconds for evaluation.
         * YAML key: {@code requiredPlaytimeDays}.
         */
        private Long requiredPlaytimeDays;

        /**
         * Alternative time field name that pairs with {@link #timeUnit} for dynamic unit selection.
         * YAML key: {@code time}.
         */
        private Long time;

        /**
         * Time unit for the {@link #time} field (seconds, minutes, hours, days, or weeks).
         * YAML key: {@code timeUnit}.
         */
        private String timeUnit;

        /**
         * Whether to use total playtime across all worlds ({@code true}) or world-specific playtime ({@code false}).
         * YAML key: {@code useTotalPlaytime}.
         */
        private Boolean useTotalPlaytime;

        /**
         * Map of world names to required playtime in seconds for world-specific validation.
         * YAML key: {@code worldPlaytimeRequirements}.
         */
        private Map<String, Long> worldPlaytimeRequirements;

        /**
         * List of world names where playtime should be tracked.
         * YAML key: {@code worlds}.
         */
        private List<String> worlds;

        /**
         * Required playtime for each world in {@link #worlds}, applied uniformly across all entries.
         * YAML key: {@code worldPlaytimeSeconds}.
         */
        private Long worldPlaytimeSeconds;

        /**
         * Required playtime for each world in minutes, converted to seconds.
         * YAML key: {@code worldPlaytimeMinutes}.
         */
        private Long worldPlaytimeMinutes;

        /**
         * Required playtime for each world in hours, converted to seconds.
         * YAML key: {@code worldPlaytimeHours}.
         */
        private Long worldPlaytimeHours;

        /**
         * Required playtime for each world in days, converted to seconds.
         * YAML key: {@code worldPlaytimeDays}.
         */
        private Long worldPlaytimeDays;

        /**
         * Optional description for this playtime requirement that can be surfaced in GUIs.
         * YAML key: {@code description}.
         */
        private String description;

        /**
         * Constructs a new {@code PlaytimeRequirementSection} bound to the supplied evaluation environment.
         *
         * @param evaluationEnvironmentBuilder the evaluation environment builder used to resolve dynamic expressions
         */
        public PlaytimeRequirementSection(
                final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder
        ) {
                super(evaluationEnvironmentBuilder);
        }

        /**
         * Resolves the required playtime in seconds, converting from any alternative units declared in configuration.
         *
         * @return the required playtime in seconds, or {@code 0L} when no global requirement is declared
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
         * Gets the time unit for the generic {@link #time} field.
         *
         * @return the normalized time unit (lowercase), defaulting to {@code "seconds"}
         */
        public String getTimeUnit() {
                return this.timeUnit != null ? this.timeUnit.toLowerCase() : "seconds";
        }

        /**
         * Gets whether to use total playtime across all worlds.
         *
         * @return {@code true} to use total playtime, {@code false} for world-specific tracking, or {@code null} when
         *         configuration should auto-detect based on world-specific settings
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
         * @return a new mutable map of world names to required playtime in seconds
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
         * Gets the required playtime for individual worlds in seconds, honoring configuration priority
         * from seconds through days.
         *
         * @return the required playtime per world in seconds, or {@code 0L} when no world requirement is declared
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
         * @return list of world names or {@code null} when no worlds are configured
         */
        @Nullable
        public List<String> getWorlds() {
                return this.worlds;
        }

        /**
         * Gets the description for this playtime requirement.
         *
         * @return the description, or {@code null} if not provided
         */
        @Nullable
        public String getDescription() {
                return this.description;
        }

        /**
         * Gets the generic time value used in combination with {@link #getTimeUnit()}.
         *
         * @return the time value, or {@code null} when undefined
         */
        @Nullable
        public Long getTime() {
                return this.time;
        }

        /**
         * Checks if this configuration has world-specific settings.
         *
         * @return {@code true} if world-specific configuration is present, otherwise {@code false}
         */
        public boolean hasWorldSpecificConfiguration() {
                return (this.worldPlaytimeRequirements != null && !this.worldPlaytimeRequirements.isEmpty()) ||
                       (this.worlds != null && !this.worlds.isEmpty() && getWorldPlaytimeSeconds() > 0);
        }

        /**
         * Validates the configuration for consistency.
         *
         * @throws IllegalStateException if the configuration omits required playtime declarations or contains
         *                               contradictory world requirements
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
         * Converts a time value to seconds based on the specified unit.
         *
         * @param timeValue the time value to convert
         * @param unit the time unit, accepting common abbreviations (e.g. {@code m}, {@code h}, {@code d})
         * @return the time in seconds, preserving the original value when the unit is already seconds
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
