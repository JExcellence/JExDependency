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

package com.raindropcentral.core.commands;

import de.jexcellence.evaluable.section.ACommandSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.NotNull;

/**
 * Command section configuration for the {@code /rc} command.
 *
 * <p>Loads command metadata from {@code resources/commands/prc.yml}.</p>
 */
public final class PRCSection extends ACommandSection {

    private static final String COMMAND_NAME = "rc";

    /**
     * Creates the command section for the central integration command.
     *
     * @param evaluationEnvironmentBuilder expression environment used by evaluable command sections
     */
    public PRCSection(final @NotNull EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
        super(COMMAND_NAME, evaluationEnvironmentBuilder);
    }
}
