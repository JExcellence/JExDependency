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

package com.raindropcentral.rdq.config.ranks.system;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

/**
 * Configuration section for various notification types related to ranks.
 * Handles configuration for rank unlock, rank tree completion, final rank unlock,
 * and cross-rank tree switch notifications.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
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
     * @param baseEnvironment the base evaluation environment builder
     */
    public NotificationSection(final EvaluationEnvironmentBuilder baseEnvironment) {
        super(baseEnvironment);
    }

    /**
     * Gets the notification configuration for unlocking a rank.
     * If not set, returns a new NotificationTypeSection with a default environment.
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
     * If not set, returns a new NotificationTypeSection with a default environment.
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
     * If not set, returns a new NotificationTypeSection with a default environment.
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
     * If not set, returns a new NotificationTypeSection with a default environment.
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
