package com.raindropcentral.rdq.command.player.rq;

import de.jexcellence.evaluable.section.ACommandSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

/**
 * Command section configuration for the "padmin" player command.
 * <p>
 * This class defines the command section for the admin system, specifying the command name
 * and providing the necessary evaluation environment for command execution.
 * </p>
 *
 * @author ItsRainingHP, JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class PRQSection extends ACommandSection {

    /**
     * The name of the bounty command.
     */
    private static final String COMMAND_NAME = "prq";

    /**
     * Constructs a new {@code PBountySection} with the specified evaluation environment builder.
     *
     * @param environmentBuilder the evaluation environment builder for command evaluation
     */
    public PRQSection(
            final EvaluationEnvironmentBuilder environmentBuilder
    ) {
        super(COMMAND_NAME, environmentBuilder);
    }
}
