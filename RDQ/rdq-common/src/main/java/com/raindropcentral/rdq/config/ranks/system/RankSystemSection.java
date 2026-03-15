package com.raindropcentral.rdq.config.ranks.system;
import com.raindropcentral.rdq.config.ranks.rank.DefaultRankSection;
import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

/**
 * Represents the RankSystemSection API type.
 */
@CSAlways
public class RankSystemSection extends AConfigSection {
	
	/**
	 * The system-wide settings for the rank system.
	 * Controls global toggles and features such as enabling the rank system,
	 * progress tracking, and broadcasts.
	 */
	private SystemSettingsSection settings;
	
	/**
	 * The configuration section defining rules and requirements for achieving the final rank.
	 * Includes options for required rank trees, minimum tiers, and alternate/ultimate ranks.
	 */
	private FinalRankRuleSection finalRankRule;
	
	/**
	 * The configuration section for progression rules in the rank system.
	 * Controls how users progress through ranks, including linearity, skipping, switching, and costs.
	 */
	private ProgressionRuleSection progressionRule;
	
	/**
	 * The configuration section for notifications related to rank events.
	 * Handles notification settings for rank unlocks, tree completions, and switches.
	 */
	private NotificationSection notification;
	
	/**
	 * The configuration section for default rank settings.
	 * Defines the default rank and rank tree that new players receive on first join.
	 */
	private DefaultRankSection defaultRank;
	
	/**
	 * Constructs a new {@code RankSystemSection} with the given evaluation environment.
	 *
	 * @param baseEnvironment the base evaluation environment for this configuration section
	 */
	public RankSystemSection(final EvaluationEnvironmentBuilder baseEnvironment) {
		super(baseEnvironment);
	}
	
	/**
	 * Returns the system settings section.
	 * If not set, returns a new {@link SystemSettingsSection} with a default environment.
	 *
	 * @return the {@link SystemSettingsSection} for global rank system settings
	 */
	public SystemSettingsSection getSettings() {
		return
			this.settings == null ?
			new SystemSettingsSection(new EvaluationEnvironmentBuilder()) :
			this.settings;
	}
	
	/**
	 * Returns the final rank rule section.
	 * If not set, returns a new {@link FinalRankRuleSection} with a default environment.
	 *
	 * @return the {@link FinalRankRuleSection} for final rank requirements and options
	 */
	public FinalRankRuleSection getFinalRankRule() {
		return
			this.finalRankRule == null ?
			new FinalRankRuleSection(new EvaluationEnvironmentBuilder()) :
			this.finalRankRule;
	}
	
	/**
	 * Returns the progression rule section.
	 * If not set, returns a new {@link ProgressionRuleSection} with a default environment.
	 *
	 * @return the {@link ProgressionRuleSection} for rank progression rules
	 */
	public ProgressionRuleSection getProgressionRule() {
		return
			this.progressionRule == null ?
			new ProgressionRuleSection(new EvaluationEnvironmentBuilder()) :
			this.progressionRule;
	}
	
	/**
	 * Returns the notification section.
	 * If not set, returns a new {@link NotificationSection} with a default environment.
	 *
	 * @return the {@link NotificationSection} for rank-related notifications
	 */
	public NotificationSection getNotification() {
		return
			this.notification == null ?
			new NotificationSection(new EvaluationEnvironmentBuilder()) :
			this.notification;
	}
	
	/**
	 * Returns the default rank section.
	 * If not set, returns a new {@link DefaultRankSection} with a default environment.
	 *
	 * @return the {@link DefaultRankSection} for default rank settings
	 */
	public DefaultRankSection getDefaultRank() {
		return
			this.defaultRank == null ?
			new DefaultRankSection(new EvaluationEnvironmentBuilder()) :
			this.defaultRank;
	}
}
