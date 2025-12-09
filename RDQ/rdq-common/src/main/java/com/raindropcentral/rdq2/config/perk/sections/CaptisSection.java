/*
package com.raindropcentral.rdq2.config.perk.sections;

import com.raindropcentral.rdq2.config.perk.PerkSection;
import com.raindropcentral.rdq2.config.perk.sections.forge.CaptisForgeSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

*/
/**
 * Configuration section for the Captis perk.
 *
 * <p>This section maps configuration values that influence the fishing catch chance
 * bonus granted by the Captis perk. The section also exposes an optional forge
 * subsection to enable more advanced tuning whenever a server integrates the
 * forge-specific behaviour.</p>
 *
 * <p>The {@link EvaluationEnvironmentBuilder} supplied to the constructor is passed to
 * the parent {@link PerkSection} so that expression evaluation for the perk shares the
 * global environment defined at load time.</p>
 *
 * @author ItsRainingHP
 * @version 1.0.1
 * @since 1.0.0
 *//*

public class CaptisSection extends PerkSection {

    */
/**
     * Rate that improves the fishing catch chance when the perk is activated.
     * The value represents the multiplier applied to the base chance (for example,
     * {@code 0.5} equals a 50% improvement).
     *//*

    @CSAlways
    private double rate;

    */
/**
     * Optional forge-specific subsection that exposes granular configuration options
     * when the server utilises Captis forge integrations.
     *//*

    @CSAlways
    private CaptisForgeSection captisForgeSection;

    */
/**
     * Constructs a new {@code CaptisSection} using the supplied evaluation environment.
     *
     * @param evaluationEnvironmentBuilder the base evaluation environment for this configuration section
     *//*

    public CaptisSection(final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
        super(evaluationEnvironmentBuilder);
    }

    */
/**
     * Gets the configured rate that improves the fishing catch chance for the perk.
     *
     * @return the improvement rate as a double (for example, {@code 0.5} for 50% improvement)
     *//*

    public double getRate() {
        return this.rate;
    }

    */
/**
     * Gets the forge section that provides advanced Captis configuration.
     *
     * @return the {@link CaptisForgeSection} instance, or {@code null} if the forge configuration is not supplied
     *//*

    public CaptisForgeSection getCaptisForgeSection() {
        return this.captisForgeSection;
    }

}*/
