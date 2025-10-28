package com.raindropcentral.rdq.config;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

public class PerkSection extends AConfigSection {

    /*@Key("max-active-perks")*/
    /*@Comment("Maximum number of active perks per player")*/
    private int maxActivePerks = 5;

    /*@Key("cooldown-seconds")*/
    /*@Comment("Default cooldown between perk activations in seconds")*/
    private int cooldownSeconds = 30;

    /*@Key("auto-enable-on-join")*/
    /*@Comment("Whether to auto-enable perks when player joins")*/
    private boolean autoEnableOnJoin = false;

    /*@Key("world-blacklist")*/
    /*@Comment("Worlds where perks cannot be used")*/
    private String[] worldBlacklist = new String[0];

    public PerkSection(EvaluationEnvironmentBuilder baseEnvironment) {
        super(baseEnvironment);
    }

    public int maxActivePerks() { return maxActivePerks; }
    public int cooldownSeconds() { return cooldownSeconds; }
    public boolean autoEnableOnJoin() { return autoEnableOnJoin; }
    public String[] worldBlacklist() { return worldBlacklist; }
}