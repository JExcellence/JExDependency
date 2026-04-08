package de.jexcellence.home.config;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

/**
 * Configuration section for teleport settings.
 *
 * @author JExcellence
 * @version 1.0.0
 */
@CSAlways
public class TeleportSection extends AConfigSection {

    private Integer delay;
    private Boolean cancelOnMove;
    private Boolean cancelOnDamage;
    private Boolean showCountdown;
    private Boolean playSounds;
    private Boolean showParticles;

    public TeleportSection(EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
        super(evaluationEnvironmentBuilder);
    }

    public int getDelay() {
        return delay != null ? delay : 3;
    }

    public boolean isCancelOnMove() {
        return cancelOnMove == null || cancelOnMove;
    }

    public boolean isCancelOnDamage() {
        return cancelOnDamage == null || cancelOnDamage;
    }

    public boolean isShowCountdown() {
        return showCountdown == null || showCountdown;
    }

    public boolean isPlaySounds() {
        return playSounds == null || playSounds;
    }

    public boolean isShowParticles() {
        return showParticles == null || showParticles;
    }
}
