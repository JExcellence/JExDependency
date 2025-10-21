package com.raindropcentral.rdq.config.ranks.system;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

/**
 * Configuration section for various notification types related to ranks.
 * Handles configuration for rank unlock, rank tree completion, final rank unlock,
 * and cross-rank tree switch notifications, ensuring that each notification
 * category is available even when the configuration omits explicit values.
 *
 * @author JExcellence
 * @version 1.0.1
 * @since 1.0.0
 */
@CSAlways
public class NotificationSection extends AConfigSection {

    /**
     * Notification configuration for unlocking a rank.
     */
    private NotificationTypeSection rankUnlock;
    /**
     * Notification configuration for completing a rank tree.
     */
    private NotificationTypeSection rankTreeComplete;
    /**
     * Notification configuration for unlocking the final rank.
     */
    private NotificationTypeSection finalRankUnlock;
    /**
     * Notification configuration for switching between rank trees.
     */
    private NotificationTypeSection crossRankTreeSwitch;

    /**
     * Constructs a NotificationSection with the given evaluation environment.
     *
     * @param baseEnvironment the base evaluation environment builder that seeds the
     *                        configuration mapper
     */
    public NotificationSection(final EvaluationEnvironmentBuilder baseEnvironment) {
        super(baseEnvironment);
    }

    /**
     * Gets the notification configuration for unlocking a rank.
     * If not set, returns a lazily created {@link NotificationTypeSection} backed by
     * a default {@link EvaluationEnvironmentBuilder} so the caller never receives
     * {@code null}.
     *
     * @return the NotificationTypeSection for rank unlock
     */
    public NotificationTypeSection getRankUnlock() {
        return
            this.rankUnlock == null ?
            new NotificationTypeSection(new EvaluationEnvironmentBuilder()) :
            this.rankUnlock;
    }

    /**
     * Gets the notification configuration for completing a rank tree.
     * If not set, returns a lazily created {@link NotificationTypeSection} backed by
     * a default {@link EvaluationEnvironmentBuilder} so the caller never receives
     * {@code null}.
     *
     * @return the NotificationTypeSection for rank tree completion
     */
    public NotificationTypeSection getRankTreeComplete() {
        return
            this.rankTreeComplete == null ?
            new NotificationTypeSection(new EvaluationEnvironmentBuilder()) :
            this.rankTreeComplete;
    }

    /**
     * Gets the notification configuration for unlocking the final rank.
     * If not set, returns a lazily created {@link NotificationTypeSection} backed by
     * a default {@link EvaluationEnvironmentBuilder} so the caller never receives
     * {@code null}.
     *
     * @return the NotificationTypeSection for final rank unlock
     */
    public NotificationTypeSection getFinalRankUnlock() {
        return
            this.finalRankUnlock == null ?
            new NotificationTypeSection(new EvaluationEnvironmentBuilder()) :
            this.finalRankUnlock;
    }

    /**
     * Gets the notification configuration for switching between rank trees.
     * If not set, returns a lazily created {@link NotificationTypeSection} backed by
     * a default {@link EvaluationEnvironmentBuilder} so the caller never receives
     * {@code null}.
     *
     * @return the NotificationTypeSection for cross-rank tree switch
     */
    public NotificationTypeSection getCrossRankTreeSwitch() {
        return
            this.crossRankTreeSwitch == null ?
            new NotificationTypeSection(new EvaluationEnvironmentBuilder()) :
            this.crossRankTreeSwitch;
    }
}
