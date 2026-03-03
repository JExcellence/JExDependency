/*
 * PRRSection.java
 *
 * @author RaindropCentral
 * @version 5.0.0
 */

package com.raindropcentral.rdr.commands;

import de.jexcellence.evaluable.section.ACommandSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.NotNull;

/**
 * Command section definition for the primary RDR player command.
 *
 * @author RaindropCentral
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
