package com.raindropcentral.rdq.database.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raindropcentral.rdq.config.perks.PluginCurrencySection;
import com.raindropcentral.rdq.config.perks.sections.forge.CaptisForgeSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import jakarta.persistence.AttributeConverter;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CaptisForgeSectionConverter implements AttributeConverter<CaptisForgeSection, String> {
	
	private static final Logger        LOGGER        = Logger.getLogger(CaptisForgeSectionConverter.class.getName());
	private static final ObjectMapper  OBJECT_MAPPER = new ObjectMapper();
	private final        ConverterTool converterTool = new ConverterTool();
	
	@Override
	public String convertToDatabaseColumn(final CaptisForgeSection captisForgeSection) {
		if (captisForgeSection == null) {
			return null;
		}
		
		try {
			final CaptisForgeSectionData data = new CaptisForgeSectionData(
				captisForgeSection.getCooldown(),
				captisForgeSection.getFishing(),
				captisForgeSection.getCost(),
				captisForgeSection.getIncome()
			);
			
			return OBJECT_MAPPER.writeValueAsString(data);
		} catch (final JsonProcessingException e) {
			LOGGER.log(Level.SEVERE, "Failed to convert CaptisForgeSectionData to JSON", e);
			throw new RuntimeException("Failed to serialize CaptisForgeSectionData", e);
		}
	}
	
	@Override
	public CaptisForgeSection convertToEntityAttribute(
		@Nullable
		final String jsonString
	) {
		if (jsonString == null || jsonString
			                          .trim()
			                          .isEmpty()) {
			return null;
		}
		
		try {
			final JsonNode                    jsonNode      = OBJECT_MAPPER.readTree(jsonString);
			final CaptisForgeSectionData data          = OBJECT_MAPPER.treeToValue(jsonNode, CaptisForgeSectionData.class);
			final CaptisForgeSection          captisSection = new CaptisForgeSection(new EvaluationEnvironmentBuilder());
			
			converterTool.setPrivateField(captisSection, "cooldown", data.cooldown, LOGGER);
			converterTool.setPrivateField(captisSection, "fishing", data.fishing, LOGGER);
			
			//TODO help with converting PluginCurrencySection
			
			return captisSection;
		} catch (final Exception e) {
			LOGGER.log(Level.SEVERE, "Failed to convert JSON to IconSection: " + jsonString, e);
			throw new RuntimeException("Failed to deserialize IconSection", e);
		}
	}
	
	private static class CaptisForgeSectionData {
		public int cooldown;
		public double                             fishing;
		public Map<String, PluginCurrencySection> cost;
		public Map<String, PluginCurrencySection> income;
		
		public CaptisForgeSectionData() {}
		
		public CaptisForgeSectionData(
			int cooldown,
			double fishing,
			Map<String, PluginCurrencySection> cost,
			Map<String, PluginCurrencySection> income
		) {
			this.cooldown = cooldown;
			this.fishing = fishing;
			this.cost = cost;
			this.income = income;
		}
	}
	
}
