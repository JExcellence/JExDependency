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

package com.raindropcentral.rdt.commands;

import de.jexcellence.evaluable.section.ACommandSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

/**
 * Command section definition for the primary player command {@code /prt}.
 *
 * <p>This class binds the command name to the underlying evaluation environment used
 * by the command framework. Command logic itself is handled by {@link PRT} and
 * registered through {@link com.raindropcentral.commands.CommandFactory}.
 */
@SuppressWarnings("unused")
public class PRTSection extends ACommandSection{
    /** Base command name players will use in chat. */
    private static final String COMMAND_NAME = "prt";

    /**
     * Create a new command section for {@code /prt} bound to the provided environment.
     *
     * @param environmentBuilder evaluation environment builder
     */
    public PRTSection(EvaluationEnvironmentBuilder environmentBuilder) {
        super(COMMAND_NAME, environmentBuilder);
    }
}
