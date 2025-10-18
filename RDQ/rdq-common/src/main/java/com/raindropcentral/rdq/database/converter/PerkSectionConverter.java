package com.raindropcentral.rdq.database.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raindropcentral.rdq.config.item.IconSection;
import com.raindropcentral.rdq.config.perk.PerkSection;
import com.raindropcentral.rdq.config.perk.PerkSettingsSection;
import com.raindropcentral.rdq.config.perk.PluginCurrencySection;
import com.raindropcentral.rdq.config.requirement.RequirementSection;
import com.raindropcentral.rdq.config.reward.RewardSection;
import com.raindropcentral.rplatform.config.permission.PermissionAmplifierSection;
import com.raindropcentral.rplatform.config.permission.PermissionCooldownSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Converter(autoApply = true)
public class PerkSectionConverter implements AttributeConverter<PerkSection, String> {
	
	private static final Logger        LOGGER         = Logger.getLogger(PerkSectionConverter.class.getName());
	private static final ObjectMapper  OBJECT_MAPPER  = new ObjectMapper();
	private static final ConverterTool CONVERTER_TOOL = new ConverterTool();
	
	@Override
	public String convertToDatabaseColumn(final PerkSection perkSection) {
		
		return "";
	}
	
	@Override
	public PerkSection convertToEntityAttribute(
		final @Nullable String jsonString
	) {
		
		if (
			jsonString == null || jsonString.trim().isEmpty()
		)
			return new PerkSection(new EvaluationEnvironmentBuilder());
		
		try {
			final JsonNode jsonNode = OBJECT_MAPPER.readTree(jsonString);
			final PerkSectionData data = OBJECT_MAPPER.treeToValue(
				jsonNode,
				PerkSectionData.class
			);
			
			final PerkSection perkSection = new PerkSection(new EvaluationEnvironmentBuilder());
			
			CONVERTER_TOOL.setPrivateField(
				perkSection,
				"icon",
				data.icon,
				LOGGER
			);
			
			CONVERTER_TOOL.setPrivateField(
				perkSection,
				"perkSettingsSection",
				data.perkSettingsSection,
				LOGGER
			);
			
			CONVERTER_TOOL.setPrivateField(
				perkSection,
				"permissionCooldownSection",
				data.permissionCooldownSection,
				LOGGER
			);
			
			CONVERTER_TOOL.setPrivateField(
				perkSection,
				"permissionAmplifierSection",
				data.permissionAmplifierSection,
				LOGGER
			);
			
			CONVERTER_TOOL.setPrivateField(
				perkSection,
				"cooldown",
				data.cooldown,
				LOGGER
			);
			
			CONVERTER_TOOL.setPrivateField(
				perkSection,
				"costSection",
				data.costSection,
				LOGGER
			);
			
			CONVERTER_TOOL.setPrivateField(
				perkSection,
				"requirementSection",
				data.requirementSection,
				LOGGER
			);
			
			CONVERTER_TOOL.setPrivateField(
				perkSection,
				"rewardSection",
				data.rewardSection,
				LOGGER
			);
			
			CONVERTER_TOOL.setPrivateField(
				perkSection,
				"requiresOwnedArea",
				data.requiresOwnedArea,
				LOGGER
			);
			
			return perkSection;
		} catch (
			  final Exception exception
		) {
			LOGGER.log(
				Level.SEVERE,
				"Failed to convert JSON to PerkSection: " + jsonString,
				exception
			);
			throw new RuntimeException(
				"Failed to deserialize PerkSection",
				exception
			);
		}
	}
	
	private static class PerkSectionData {
		
		public IconSection icon;
		public PerkSettingsSection perkSettingsSection;
		public PermissionCooldownSection permissionCooldownSection;
		public PermissionAmplifierSection permissionAmplifierSection;
		public Integer                    cooldown;
		public Map<String, PluginCurrencySection> costSection;
		public Map<String, RequirementSection>    requirementSection;
		public Map<String, RewardSection>         rewardSection;
		public Boolean                            requiresOwnedArea;
		
		public PerkSectionData() {}
		
		public PerkSectionData(
			final @Nullable IconSection icon,
			final @Nullable PerkSettingsSection perkSettingsSection,
			final @Nullable PermissionCooldownSection permissionCooldownSection,
			final @Nullable PermissionAmplifierSection permissionAmplifierSection,
			final @Nullable Integer cooldown,
			final @Nullable Map<String, PluginCurrencySection> costSection,
			final @Nullable Map<String, RequirementSection> requirementSection,
			final @Nullable Map<String, RewardSection> rewardSection,
			final @Nullable Boolean requiresOwnedArea
		) {
			
			this.icon = icon;
			this.perkSettingsSection = perkSettingsSection;
			this.permissionCooldownSection = permissionCooldownSection;
			this.permissionAmplifierSection = permissionAmplifierSection;
			this.cooldown = cooldown;
			this.costSection = costSection;
			this.requirementSection = requirementSection;
			this.rewardSection = rewardSection;
			this.requiresOwnedArea = requiresOwnedArea;
		}
		
	}
	
}