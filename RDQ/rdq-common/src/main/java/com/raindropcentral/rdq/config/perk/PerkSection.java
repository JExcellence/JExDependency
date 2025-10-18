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
 * This section defines common properties for all perks, such as display name, description,
 * icon, cooldown, cost, land requirements, requirements to obtain the perk, and rewards.
 * It also provides utility methods for accessing these properties and for retrieving
 * default potion types.
 * </p>
 *
 * @author ItsRainingHP
 * @version 1.0.0
 * @since TBD
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
	public PerkSection(
		final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder
	) {
		
		super(evaluationEnvironmentBuilder);
	}
	
	public PerkSettingsSection getPerkSettings() {
		return
			this.perkSettings == null ?
			new PerkSettingsSection(new EvaluationEnvironmentBuilder()) :
			this.perkSettings;
	}
	
	public PermissionCooldownSection getPermissionCooldowns() {
		
		return
			this.permissionCooldowns == null ?
			new PermissionCooldownSection(new EvaluationEnvironmentBuilder()) :
			this.permissionCooldowns;
	}
	
	public PermissionAmplifierSection getPermissionAmplifiers() {
		
		return
			this.permissionAmplifiers == null ?
			new PermissionAmplifierSection(new EvaluationEnvironmentBuilder()) :
			this.permissionAmplifiers;
	}
	
	public PermissionDurationSection getPermissionDurations() {
		
		return
			this.permissionDurations == null ?
			new PermissionDurationSection(new EvaluationEnvironmentBuilder()) :
			this.permissionDurations;
	}
	
	public Map<String, PluginCurrencySection> getCosts() {
		
		return
			this.costs == null ?
			new HashMap<>() :
			this.costs;
	}
	
	public Map<String, RequirementSection> getRequirements() {
		
		return
			this.requirements == null ?
			new HashMap<>() :
			this.requirements;
	}
	
	public Map<String, RewardSection> getRewards() {
		
		return
			this.rewards == null ?
			new HashMap<>() :
			this.rewards;
	}
}