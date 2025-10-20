package com.raindropcentral.rdq.command.player.rq;

import de.jexcellence.evaluable.section.ACommandSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.NotNull;

/**
 * Declares the configuration block that drives the {@code /prq} root command.
 * <p>
 * The section exposes the command name and supplies the evaluation environment
 * used by {@link PRQ} to resolve parameter parsers and permission nodes at
 * runtime. It inherits the wiring capabilities from {@link ACommandSection},
 * allowing the {@code /prq} command to share the same bootstrap semantics as
 * the rest of the RDQ command suite.
 * </p>
 * <p>
 * Besides capturing the canonical name, the section centralises construction of
 * the {@link EvaluationEnvironmentBuilder} so that the command runtime can
 * depend on a single, well-defined context when resolving dependencies.
 * </p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public final class PRQSection extends ACommandSection {

    /**
     * Canonical command name consumed by the command factory when wiring the
     * handler.
     */
    private static final String COMMAND_NAME = "prq";

    /**
     * Builds the command section with the supplied evaluation environment.
     * <p>
     * The environment builder is forwarded to {@link ACommandSection} so that
     * parser lookup, permission resolution, and dependency retrieval happen
     * through the centralised evaluation pipeline.
     * </p>
     *
     * @param environmentBuilder builder supplying parsers and dependency lookups
     *                            needed by the command runtime
     */
    public PRQSection(final @NotNull EvaluationEnvironmentBuilder environmentBuilder) {
        super(COMMAND_NAME, environmentBuilder);
    }
}