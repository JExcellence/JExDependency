/*
package com.raindropcentral.rdq2.config.perk;

import com.raindropcentral.rdq2.config.requirement.BaseRequirementSection;
import com.raindropcentral.rdq2.config.reward.RewardSection;
import com.raindropcentral.rplatform.config.permission.PermissionAmplifierSection;
import com.raindropcentral.rplatform.config.permission.PermissionCooldownSection;
import com.raindropcentral.rplatform.config.permission.PermissionDurationSection;
import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.HashMap;
import java.util.Map;


@CSAlways
public class PerkSection extends AConfigSection {

    private PerkSettingsSection perkSettings;

    private PermissionCooldownSection permissionCooldowns;

    private PermissionAmplifierSection permissionAmplifiers;

    private PermissionDurationSection permissionDurations;

    private Map<String, PluginCurrencySection> costs;

    private Map<String, BaseRequirementSection> requirements;

    private Map<String, RewardSection> rewards;

    public PerkSection(EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
        super(evaluationEnvironmentBuilder);
    }

    public PerkSettingsSection getPerkSettings() {
        return perkSettings == null
                ? new PerkSettingsSection(new EvaluationEnvironmentBuilder())
                : perkSettings;
    }

    public PermissionCooldownSection getPermissionCooldowns() {
        return permissionCooldowns == null
                ? new PermissionCooldownSection(new EvaluationEnvironmentBuilder())
                : permissionCooldowns;
    }

    public PermissionAmplifierSection getPermissionAmplifiers() {
        return permissionAmplifiers == null
                ? new PermissionAmplifierSection(new EvaluationEnvironmentBuilder())
                : permissionAmplifiers;
    }

    public PermissionDurationSection getPermissionDurations() {
        return permissionDurations == null
                ? new PermissionDurationSection(new EvaluationEnvironmentBuilder())
                : permissionDurations;
    }

    public Map<String, PluginCurrencySection> getCosts() {
        return costs == null ? new HashMap<>() : costs;
    }

    public Map<String, BaseRequirementSection> getRequirements() {
        return requirements == null ? new HashMap<>() : requirements;
    }

    public Map<String, RewardSection> getRewards() {
        return rewards == null ? new HashMap<>() : rewards;
    }
}
*/
