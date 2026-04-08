package de.jexcellence.home.command.admin;

import de.jexcellence.evaluable.section.ACommandSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

/**
 * Command section configuration for the admin home command.
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class PAdminHomeSection extends ACommandSection {

    private static final String COMMAND_NAME = "homeadmin";

    public PAdminHomeSection(EvaluationEnvironmentBuilder environmentBuilder) {
        super(COMMAND_NAME, environmentBuilder);
    }
}
