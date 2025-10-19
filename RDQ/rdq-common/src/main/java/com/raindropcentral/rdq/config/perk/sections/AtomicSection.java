package com.raindropcentral.rdq.config.perk.sections;

import com.raindropcentral.rdq.config.perk.PerkSection;
import com.raindropcentral.rdq.config.perk.sections.forge.AtomicForgeSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

/**
 * Configuration section for the Atomic perk.
 * <p>
 * This section defines the parameters for the atomic machine, including XP requirements,
 * sound and particle effects, required supporting block, notification timer, and
 * advanced forge configuration.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.1
 * @since 1.0.0
 */
public class AtomicSection extends PerkSection {

    /**
     * XP necessary to trigger the machine.
     */
    @CSAlways
    private int xp;

    /**
     * If {@code true}, plays sounds on bottle generation.
     */
    @CSAlways
    private boolean sound;

    /**
     * If {@code true}, activates particles on bottle generation.
     */
    @CSAlways
    private boolean particles;

    /**
     * Block required to support and stabilize the machine (must surround it in-game).
     */
    @CSAlways
    private String block;

    /**
     * Timer before notifying the player they do not have enough XP (in seconds).
     */
    @CSAlways
    private int notify;

    /**
     * Configuration section for advanced atomic forge settings.
     */
    @CSAlways
    private AtomicForgeSection atomicForgeSection;

    /**
     * Constructs a new {@code AtomicSection} with the specified evaluation environment.
     *
     * @param evaluationEnvironmentBuilder the base evaluation environment for this configuration section
     */
    public AtomicSection(final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
        super(evaluationEnvironmentBuilder);
    }

    /**
     * Gets the amount of XP necessary to trigger the atomic machine.
     *
     * @return the required XP as an integer
     */
    public int getXp() {
        return this.xp;
    }

    /**
     * Determines whether sound effects are played on bottle generation.
     *
     * @return {@code true} if sound effects are enabled; {@code false} otherwise
     */
    public boolean isSound() {
        return this.sound;
    }

    /**
     * Determines whether particle effects are activated on bottle generation.
     *
     * @return {@code true} if particle effects are enabled; {@code false} otherwise
     */
    public boolean isParticles() {
        return this.particles;
    }

    /**
     * Gets the block type required to support and stabilize the atomic machine.
     *
     * @return the block type as a string
     */
    public String getBlock() {
        return this.block;
    }

    /**
     * Gets the timer duration before notifying the player that they do not have enough XP.
     *
     * @return the notification timer in seconds
     */
    public int getNotify() {
        return this.notify;
    }

    /**
     * Gets the configuration section for advanced atomic forge settings.
     *
     * @return the {@link AtomicForgeSection} instance
     */
    public AtomicForgeSection getAtomicForgeSection() {
        return this.atomicForgeSection;
    }
}
