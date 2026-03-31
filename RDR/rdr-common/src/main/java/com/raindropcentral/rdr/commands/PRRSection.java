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

package com.raindropcentral.rdr.commands;

import de.jexcellence.evaluable.section.ACommandSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.NotNull;

/**
 * Command section definition for the primary RDR player command.
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
@SuppressWarnings("unused")
public class PRRSection extends ACommandSection {

    private static final String COMMAND_NAME = "prr";

    /**
     * Creates the command section.
     *
     * @param environmentBuilder expression environment backing the section
     */
    public PRRSection(final @NotNull EvaluationEnvironmentBuilder environmentBuilder) {
        super(COMMAND_NAME, environmentBuilder);
    }
}