package com.raindropcentral.rdq.config.perk;

import com.raindropcentral.rdq.config.requirement.RequirementSection;
import com.raindropcentral.rdq.config.reward.RewardSection;
import com.raindropcentral.rplatform.config.permission.PermissionAmplifierSection;
import com.raindropcentral.rplatform.config.permission.PermissionCooldownSection;
import com.raindropcentral.rplatform.config.permission.PermissionDurationSection;
import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Base configuration section for a perk.
 * <p>
 * This section defines the shared configuration fragments used across perks including the
 * settings, cooldown modifiers, amplifier modifiers, duration modifiers, costs, requirements,
 * and rewards. It exposes convenience accessors that guarantee a non-{@code null} section is
 * returned even when the backing configuration omits the corresponding node.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.1
 * @since 1.0.0
 */
@CSAlways
public class PerkSection extends AConfigSection {

    private PerkSettingsSection perkSettings;

    private PermissionCooldownSection permissionCooldowns;

    private PermissionAmplifierSection permissionAmplifiers;

    private PermissionDurationSection permissionDurations;

    private Map<String, PluginCurrencySection> costs;

    private Map<String, RequirementSection> requirements;

    private Map<String, RewardSection> rewards;

    /**
     * Constructs a new {@code PerkSection} with the specified evaluation environment.
     *
     * @param evaluationEnvironmentBuilder the evaluation environment builder for this configuration section
     */
    public PerkSection(final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
        super(evaluationEnvironmentBuilder);
    }

    /**
     * Provides the perk settings configuration.
     *
     * @return a non-{@code null} {@link PerkSettingsSection}
     */
    public PerkSettingsSection getPerkSettings() {
        return this.perkSettings == null
                ? new PerkSettingsSection(new EvaluationEnvironmentBuilder())
                : this.perkSettings;
    }

    /**
     * Supplies the permission cooldown configuration.
     *
     * @return a non-{@code null} {@link PermissionCooldownSection}
     */
    public PermissionCooldownSection getPermissionCooldowns() {
        return this.permissionCooldowns == null
                ? new PermissionCooldownSection(new EvaluationEnvironmentBuilder())
                : this.permissionCooldowns;
    }

    /**
     * Supplies the permission amplifier configuration.
     *
     * @return a non-{@code null} {@link PermissionAmplifierSection}
     */
    public PermissionAmplifierSection getPermissionAmplifiers() {
        return this.permissionAmplifiers == null
                ? new PermissionAmplifierSection(new EvaluationEnvironmentBuilder())
                : this.permissionAmplifiers;
    }

    /**
     * Supplies the permission duration configuration.
     *
     * @return a non-{@code null} {@link PermissionDurationSection}
     */
    public PermissionDurationSection getPermissionDurations() {
        return this.permissionDurations == null
                ? new PermissionDurationSection(new EvaluationEnvironmentBuilder())
                : this.permissionDurations;
    }

    /**
     * Retrieves the configured perk costs.
     *
     * @return a mutable map of currency identifiers to {@link PluginCurrencySection} entries
     */
    public Map<String, PluginCurrencySection> getCosts() {
        return this.costs == null ? new HashMap<>() : this.costs;
    }

    /**
     * Retrieves the configured perk requirements.
     *
     * @return a mutable map of requirement identifiers to {@link RequirementSection} entries
     */
    public Map<String, RequirementSection> getRequirements() {
        return this.requirements == null ? new HashMap<>() : this.requirements;
    }

    /**
     * Retrieves the configured perk rewards.
     *
     * @return a mutable map of reward identifiers to {@link RewardSection} entries
     */
    public Map<String, RewardSection> getRewards() {
        return this.rewards == null ? new HashMap<>() : this.rewards;
    }
}
