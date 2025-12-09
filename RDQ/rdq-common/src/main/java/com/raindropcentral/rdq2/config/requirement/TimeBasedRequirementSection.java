package com.raindropcentral.rdq2.config.requirement;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration section for time-based requirements.
 * <p>
 * This section handles all configuration options specific to {@code TimeBasedRequirement},
 * including time constraints, cooldowns, scheduling, and time zone handling.
 * </p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@CSAlways
public class TimeBasedRequirementSection extends AConfigSection {

    /**
     * Time constraint in seconds.
     * <p>
     * YAML key: {@code timeConstraintSeconds}.
     * </p>
     */
    private Long timeConstraintSeconds;

    /**
     * Cooldown period in seconds.
     * <p>
     * YAML key: {@code cooldownSeconds}.
     * </p>
     */
    private Long cooldownSeconds;

    /**
     * Start time for time-based availability (HH:mm format).
     * <p>
     * YAML key: {@code startTime}.
     * </p>
     */
    private String startTime;

    /**
     * End time for time-based availability (HH:mm format).
     * <p>
     * YAML key: {@code endTime}.
     * </p>
     */
    private String endTime;

    /**
     * Time zone for time calculations.
     * <p>
     * YAML key: {@code timeZone}.
     * </p>
     */
    private String timeZone;

    /**
     * Whether this requirement is recurring (resets after completion).
     * <p>
     * YAML key: {@code recurring}.
     * </p>
     */
    private Boolean recurring;

    /**
     * Days of the week when this requirement is active (1=Monday, 7=Sunday).
     * <p>
     * YAML key: {@code activeDays}.
     * </p>
     */
    private List<Integer> activeDays;

    /**
     * Specific dates when this requirement is active (yyyy-MM-dd format).
     * <p>
     * YAML key: {@code activeDates}.
     * </p>
     */
    private List<String> activeDates;

    /**
     * Whether to check real-world time or server uptime.
     * <p>
     * YAML key: {@code useRealTime}.
     * </p>
     */
    private Boolean useRealTime;

    /**
     * Constructs a new time-based requirement section.
     *
     * @param evaluationEnvironmentBuilder the evaluation environment builder
     */    /**
     * Default no-arg constructor for Jackson deserialization.
     */
    protected TimeBasedRequirementSection() {
        super(new EvaluationEnvironmentBuilder());
    }

    public TimeBasedRequirementSection(
            final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder
    ) {
        super(evaluationEnvironmentBuilder);
    }

    /**
     * Gets the time constraint in seconds.
     *
     * @return the configured time constraint, or {@code 0L} when unspecified
     */
    public Long getTimeConstraintSeconds() {
        if (this.timeConstraintSeconds != null) {
            return this.timeConstraintSeconds;
        }
        return 0L;
    }

    /**
     * Gets the cooldown in seconds.
     *
     * @return the configured cooldown, or {@code 0L} when unspecified
     */
    public Long getCooldownSeconds() {
        if (this.cooldownSeconds != null) {
            return this.cooldownSeconds;
        }
        return 0L;
    }

    /**
     * Gets the start time for availability.
     *
     * @return the start time in {@code HH:mm} format, or {@code null} when unset
     */
    public String getStartTime() {
        return this.startTime;
    }

    /**
     * Gets the end time for availability.
     *
     * @return the end time in {@code HH:mm} format, or {@code null} when unset
     */
    public String getEndTime() {
        return this.endTime;
    }

    /**
     * Gets the time zone for time calculations.
     *
     * @return the configured time zone, or {@code "UTC"} when unspecified
     */
    public String getTimeZone() {
        return this.timeZone != null ?
               this.timeZone :
               "UTC";
    }

    /**
     * Determines whether this requirement is recurring.
     *
     * @return {@code true} when the requirement resets after completion; otherwise {@code false}
     */
    public Boolean getRecurring() {
        return this.recurring != null ?
               this.recurring :
               false;
    }

    /**
     * Gets the active days of the week.
     *
     * @return list of active days (1=Monday, 7=Sunday); empty when unspecified
     */
    public List<Integer> getActiveDays() {
        return this.activeDays != null ?
               this.activeDays :
               new ArrayList<>();
    }

    /**
     * Gets the specific active dates.
     *
     * @return list of active dates in {@code yyyy-MM-dd} format; empty when unspecified
     */
    public List<String> getActiveDates() {
        return this.activeDates != null ?
               this.activeDates :
               new ArrayList<>();
    }

    /**
     * Determines whether to use real-world time or server uptime.
     *
     * @return {@code true} for real-world time, or {@code false} for server uptime (default {@code true})
     */
    public Boolean getUseRealTime() {
        return this.useRealTime != null ?
               this.useRealTime :
               true;
    }

}
