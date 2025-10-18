package com.raindropcentral.rdq.command.player.rq;

import de.jexcellence.evaluable.section.ACommandSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.NotNull;

/**
 * Command section configuration for the "prq" player command.
 * <p>
 * This class defines the command section for the RaindropQuests system,
 * specifying the command name and providing the necessary evaluation
 * environment for command execution.
 * </p>
 *
 * @author ItsRainingHP, JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public final class PRQSection extends ACommandSection {

    private static final String COMMAND_NAME = "prq";

    public PRQSection(final @NotNull EvaluationEnvironmentBuilder environmentBuilder) {
        super(COMMAND_NAME, environmentBuilder);
    }
}