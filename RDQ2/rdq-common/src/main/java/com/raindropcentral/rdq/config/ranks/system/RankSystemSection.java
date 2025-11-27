package com.raindropcentral.rdq.config.ranks.system;
import com.raindropcentral.rdq.config.ranks.rank.DefaultRankSection;
import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

/**
 * Aggregates the configuration subsections that define the behaviour of the rank system.
 * <p>
 * Each subsection focuses on a specific aspect of rank management, including global
 * toggles, progression rules, notification handling, and the default rank assigned to
 * new players. The section is instantiated with a base {@link EvaluationEnvironmentBuilder}
 * which is propagated to child sections when they are present, while sensible defaults are
 * supplied when they are not explicitly configured.
 * </p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@CSAlways
public class RankSystemSection extends AConfigSection {

        /**
         * The system-wide settings for the rank system.
         * <p>
         * Controls global toggles and features such as enabling the rank system,
         * progress tracking, and broadcasts. When {@code null}, a default
         * {@link SystemSettingsSection} is created on demand using a fresh evaluation
         * environment builder.
         * </p>
         */
        private SystemSettingsSection settings;

        /**
         * The configuration section defining rules and requirements for achieving the final rank.
         * <p>
         * Includes options for required rank trees, minimum tiers, and alternate or ultimate
         * ranks. If this section is absent in the configuration, calls to {@link #getFinalRankRule()}
         * yield a lazily created instance backed by a default evaluation environment.
         * </p>
         */
        private FinalRankRuleSection finalRankRule;

        /**
         * The configuration section for progression rules in the rank system.
         * <p>
         * Controls how users progress through ranks, including linearity, skipping, switching, and
         * costs. Absent configuration data triggers the creation of a default
         * {@link ProgressionRuleSection} when accessed.
         * </p>
         */
        private ProgressionRuleSection progressionRule;

        /**
         * The configuration section for notifications related to rank events.
         * <p>
         * Handles notification settings for rank unlocks, tree completions, and switches. When not
         * explicitly configured, {@link #getNotification()} supplies a default instance with a new
         * evaluation environment builder.
         * </p>
         */
        private NotificationSection notification;

        /**
         * The configuration section for default rank settings.
         * <p>
         * Defines the default rank and rank tree that new players receive on first join. Accessing
         * {@link #getDefaultRank()} produces a default configuration section if the value was not
         * provided.
         * </p>
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
         * <p>
         * If not set, returns a new {@link SystemSettingsSection} with a default evaluation
         * environment builder. The newly created instance is not cached, ensuring callers receive
         * a fresh view of the defaults when the configuration is absent.
         * </p>
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
         * <p>
         * If not set, returns a new {@link FinalRankRuleSection} that is constructed with a new
         * evaluation environment builder to ensure expression evaluation is possible without
         * explicit configuration.
         * </p>
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
         * <p>
         * If not set, returns a new {@link ProgressionRuleSection} with a default evaluation
         * environment builder, ensuring rank progression rules are available even when the
         * configuration omits this section.
         * </p>
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
         * <p>
         * If not set, returns a new {@link NotificationSection} with a default evaluation
         * environment builder. This allows rank-related notifications to fall back to sensible
         * defaults.
         * </p>
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
         * <p>
         * If not set, returns a new {@link DefaultRankSection} with a default evaluation
         * environment builder so that new players can still be assigned a baseline rank.
         * </p>
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