package com.raindropcentral.rdq.config.ranks.system;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

/**
 * Configuration section for system-wide settings related to the rank system.
 * <p>
 * This section controls toggles that activate or deactivate behaviour such as the
 * overall rank system, final rank rules, cross-tree progression, progress tracking,
 * and rank broadcasts. Each flag defaults to {@code true} when no explicit value is
 * provided in the configuration file.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.1
 * @since 1.0.0
 */
@CSAlways
public class SystemSettingsSection extends AConfigSection {
	
	/**
	 * Whether the rank system is enabled.
	 * <p>
	 * If {@code null}, the rank system is considered enabled by default.
	 * </p>
	 */
	private Boolean enableRankSystem;
	
	/**
	 * Whether the final rank rules are enabled.
	 * <p>
	 * If {@code null}, final rank rules are considered enabled by default.
	 * </p>
	 */
	private Boolean enableFinalRankRules;
	
	/**
	 * Whether cross-tree progression is enabled.
	 * <p>
	 * If {@code null}, cross-tree progression is considered enabled by default.
	 * </p>
	 */
	private Boolean enableCrossTreeProgression;
	
	/*
	 * Whether rank switching is enabled.
	 * <p>
	 * Currently commented out and not in use.
	 * </p>
	 */
	// private Boolean enableRankSwitching;
	
	/**
	 * Whether progress tracking is enabled.
	 * <p>
	 * If {@code null}, progress tracking is considered enabled by default.
	 * </p>
	 */
	private Boolean enableProgressTracking;
	
	/**
	 * Whether rank broadcasts are enabled.
	 * <p>
	 * If {@code null}, rank broadcasts are considered enabled by default.
	 * </p>
	 */
	private Boolean enableRankBroadcasts;
	
        /**
         * Constructs a new {@code SystemSettingsSection} with the specified evaluation environment.
         *
         * @param baseEnvironment the base {@link EvaluationEnvironmentBuilder} used to seed inherited configuration evaluation
         */
        public SystemSettingsSection(final EvaluationEnvironmentBuilder baseEnvironment) {
                super(baseEnvironment);
        }
	
        /**
         * Indicates whether the rank system is enabled.
         * <p>
         * The configuration treats a {@code null} value as enabled, so this method never
         * returns {@code null} and defaults to {@code true} when the flag is unspecified.
         * </p>
         *
         * @return {@code true} when the rank system is enabled or unspecified; {@code false} when explicitly disabled
         */
        public Boolean getEnableRankSystem() {
                return this.enableRankSystem == null || this.enableRankSystem;
        }

        /**
         * Indicates whether the final rank rules are enabled.
         * <p>
         * A {@code null} configuration entry defaults to {@code true}, ensuring the rules
         * remain active unless explicitly disabled.
         * </p>
         *
         * @return {@code true} when the final rank rules are enabled or unspecified; {@code false} when explicitly disabled
         */
        public Boolean getEnableFinalRankRules() {
                return this.enableFinalRankRules == null || this.enableFinalRankRules;
        }

        /**
         * Indicates whether cross-tree progression is enabled.
         * <p>
         * A {@code null} value is treated as enabled, so cross-tree progression remains
         * active by default.
         * </p>
         *
         * @return {@code true} when cross-tree progression is enabled or unspecified; {@code false} when explicitly disabled
         */
        public Boolean getEnableCrossTreeProgression() {
                return this.enableCrossTreeProgression == null || this.enableCrossTreeProgression;
        }

        /**
         * Indicates whether progress tracking is enabled.
         * <p>
         * Progress tracking defaults to {@code true} when the configuration omits the flag,
         * and this method never returns {@code null}.
         * </p>
         *
         * @return {@code true} when progress tracking is enabled or unspecified; {@code false} when explicitly disabled
         */
        public Boolean getEnableProgressTracking() {
                return this.enableProgressTracking == null || this.enableProgressTracking;
        }

        /**
         * Indicates whether rank broadcasts are enabled.
         * <p>
         * Rank broadcasts default to {@code true} when not explicitly configured, meaning
         * this method never returns {@code null}.
         * </p>
         *
         * @return {@code true} when rank broadcasts are enabled or unspecified; {@code false} when explicitly disabled
         */
        public Boolean getEnableRankBroadcasts() {
                return this.enableRankBroadcasts == null || this.enableRankBroadcasts;
        }
}