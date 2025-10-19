package com.raindropcentral.rdq.config.perk.sections.forge;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

/**
 * Configuration section that exposes the Atomic Accelerator perk tuning knobs.
 * <p>
 * Behaviour: loads the {@code atomic-accelerator.rate} node eagerly thanks to the
 * {@link CSAlways} marker so the perk calculation can safely assume a populated
 * value during runtime. Failure modes: the mapper falls back to {@code 0.0} when
 * the configuration omits the node or provides a non-numeric value. Asynchronous
 * considerations: instances are created during synchronous configuration bootstrapping,
 * but the resulting data object is immutable and safe to consume from any thread.
 * </p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.1
 */
public class AtomicAcceleratorSection extends AConfigSection {

        /**
         * Rate multiplier applied to shorten the atomic reactor's timer.
         * Behaviour: supplied directly to perk logic as a scalar multiplier for the
         * accelerator bonus. Failure modes: defaults to {@code 0.0} when the config
         * omits the node or provides malformed input. Asynchronous considerations:
         * read-only after construction and safe to access from background threads.
         */
        @CSAlways
        private double rate;

        /**
         * Constructs the section using the supplied evaluation environment builder.
         * Behaviour: passes the builder to the superclass so value resolution honours
         * any registered interpolations or validators. Failure modes: none, provided
         * {@code baseEnvironment} is non-null. Asynchronous considerations: invoked
         * on the synchronous configuration loading thread.
         *
         * @param baseEnvironment the base evaluation environment for this configuration section
         */
        public AtomicAcceleratorSection(final EvaluationEnvironmentBuilder baseEnvironment) {
                super(baseEnvironment);
        }

        /**
         * Gets the configured multiplier for the accelerator's timer reduction.
         * Behaviour: returns the raw scalar used by downstream perk logic without
         * additional transformation. Failure modes: none; returns {@code 0.0} when
         * configuration did not supply a value. Asynchronous considerations: thread-safe
         * because the section is immutable after construction.
         *
         * @return the improvement rate as a double
         */
        public double getRate() {
                return this.rate;
        }

}