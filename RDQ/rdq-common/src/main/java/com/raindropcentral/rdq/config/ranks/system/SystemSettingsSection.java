package com.raindropcentral.rdq.config.ranks.system;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

/**
 * Configuration section for system-wide settings related to the rank system.
 *
 * <p>This section controls various toggles for enabling or disabling features
 * such as the rank system, final rank rules, cross-tree progression, progress tracking,
 * and rank broadcasts.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
@CSAlways
public class SystemSettingsSection extends AConfigSection {
	
	/**
	 * Whether the rank system is enabled.
 *
 * <p>If {@code null}, the rank system is considered enabled by default.
	 */
	private Boolean enableRankSystem;
	
	/**
	 * Whether the final rank rules are enabled.
 *
 * <p>If {@code null}, final rank rules are considered enabled by default.
	 */
	private Boolean enableFinalRankRules;
	
	/**
	 * Whether cross-tree progression is enabled.
 *
 * <p>If {@code null}, cross-tree progression is considered enabled by default.
	 */
	private Boolean enableCrossTreeProgression;
	
	/*
	 * Whether rank switching is enabled.
 *
 * <p>Currently commented out and not in use.
	 */
	// private Boolean enableRankSwitching;
	
	/**
	 * Whether progress tracking is enabled.
 *
 * <p>If {@code null}, progress tracking is considered enabled by default.
	 */
	private Boolean enableProgressTracking;
	
	/**
	 * Whether rank broadcasts are enabled.
 *
 * <p>If {@code null}, rank broadcasts are considered enabled by default.
	 */
	private Boolean enableRankBroadcasts;
	
	/**
	 * Whether navigation buttons (up/down) should be inverted in the rank path overview.
 *
 * <p>When {@code true}, the up button moves down and the down button moves up.
	 * This is useful for users who prefer inverted navigation (common in some regions).
	 * If {@code null}, inverted navigation is disabled by default.
	 */
	private Boolean invertNavigation;
	
	/**
	 * Constructs a new {@code SystemSettingsSection} with the specified evaluation environment.
	 *
	 * @param baseEnvironment the base evaluation environment for this configuration section
	 */
	public SystemSettingsSection(final EvaluationEnvironmentBuilder baseEnvironment) {
		super(baseEnvironment);
	}
	
	/**
	 * Returns whether the rank system is enabled.
 *
 * <p>If the value is {@code null}, the rank system is considered enabled by default.
	 *
	 * @return {@code true} if the rank system is enabled or not explicitly disabled; {@code false} otherwise
	 */
	public Boolean getEnableRankSystem() {
		return this.enableRankSystem == null || this.enableRankSystem;
	}
	
	/**
	 * Returns whether the final rank rules are enabled.
 *
 * <p>If the value is {@code null}, final rank rules are considered enabled by default.
	 *
	 * @return {@code true} if final rank rules are enabled or not explicitly disabled; {@code false} otherwise
	 */
	public Boolean getEnableFinalRankRules() {
		return this.enableFinalRankRules == null || this.enableFinalRankRules;
	}
	
	/**
	 * Returns whether cross-tree progression is enabled.
 *
 * <p>If the value is {@code null}, cross-tree progression is considered enabled by default.
	 *
	 * @return {@code true} if cross-tree progression is enabled or not explicitly disabled; {@code false} otherwise
	 */
	public Boolean getEnableCrossTreeProgression() {
		return this.enableCrossTreeProgression == null || this.enableCrossTreeProgression;
	}
	
	/**
	 * Returns whether progress tracking is enabled.
 *
 * <p>If the value is {@code null}, progress tracking is considered enabled by default.
	 *
	 * @return {@code true} if progress tracking is enabled or not explicitly disabled; {@code false} otherwise
	 */
	public Boolean getEnableProgressTracking() {
		return this.enableProgressTracking == null || this.enableProgressTracking;
	}
	
	/**
	 * Returns whether rank broadcasts are enabled.
 *
 * <p>If the value is {@code null}, rank broadcasts are considered enabled by default.
	 *
	 * @return {@code true} if rank broadcasts are enabled or not explicitly disabled; {@code false} otherwise
	 */
	public Boolean getEnableRankBroadcasts() {
		return this.enableRankBroadcasts == null || this.enableRankBroadcasts;
	}
	
	/**
	 * Returns whether navigation buttons should be inverted.
 *
 * <p>When inverted, the up button moves down and the down button moves up.
	 * If the value is {@code null}, inverted navigation is disabled by default.
	 *
	 * @return {@code true} if navigation should be inverted; {@code false} otherwise
	 */
	public Boolean getInvertNavigation() {
		return this.invertNavigation != null && this.invertNavigation;
	}
}
