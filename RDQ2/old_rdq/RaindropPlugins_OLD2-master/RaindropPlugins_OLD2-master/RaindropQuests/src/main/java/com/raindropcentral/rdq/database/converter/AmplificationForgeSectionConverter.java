package com.raindropcentral.rdq.database.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raindropcentral.rdq.config.perks.PluginCurrencySection;
import com.raindropcentral.rdq.config.perks.sections.forge.AmplificationForgeSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import jakarta.persistence.AttributeConverter;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AmplificationForgeSectionConverter implements AttributeConverter<AmplificationForgeSection, String> {
	
	private static final Logger        LOGGER        = Logger.getLogger(AmplificationForgeSectionConverter.class.getName());
	private static final ObjectMapper  OBJECT_MAPPER = new ObjectMapper();
	private final        ConverterTool converterTool = new ConverterTool();
	
	@Override
	public String convertToDatabaseColumn(final AmplificationForgeSection amplificationForgeSection) {
		if (amplificationForgeSection == null) {
			return null;
		}
		
		try {
			final AmplificationForgeSectionData data = new AmplificationForgeSectionData(
				amplificationForgeSection.getChance(),
				amplificationForgeSection.getRate(),
				amplificationForgeSection.getDistance(),
				amplificationForgeSection.getCost()
			);
			
			return OBJECT_MAPPER.writeValueAsString(data);
		} catch (final JsonProcessingException e) {
			LOGGER.log(Level.SEVERE, "Failed to convert AtomicForgeSectionData to JSON", e);
			throw new RuntimeException("Failed to serialize AtomicForgeSectionData", e);
		}
	}
	
	@Override
	public AmplificationForgeSection convertToEntityAttribute(
		@Nullable
		final String jsonString
	) {
		if (jsonString == null || jsonString.trim().isEmpty()) {
			return null;
		}
		
		try {
			final JsonNode jsonNode = OBJECT_MAPPER.readTree(jsonString);
			final AmplificationForgeSectionData data = OBJECT_MAPPER.treeToValue(jsonNode, AmplificationForgeSectionData.class);
			final AmplificationForgeSection amplificationSection = new AmplificationForgeSection(new EvaluationEnvironmentBuilder());
			
			converterTool.setPrivateField(amplificationSection, "chance", data.chance, LOGGER);
			converterTool.setPrivateField(amplificationSection, "rate", data.rate, LOGGER);
			converterTool.setPrivateField(amplificationSection, "distance", data.distance, LOGGER);
			
			//TODO help with converting PluginCurrencySection
			
			return amplificationSection;
		} catch (final Exception e) {
			LOGGER.log(Level.SEVERE, "Failed to convert JSON to AmplificationForgeSectionData: " + jsonString, e);
			throw new RuntimeException("Failed to deserialize AmplificationForgeSectionData", e);
		}
	}
	
	private static class AmplificationForgeSectionData {
		public double chance;
		public double rate;
		public int                                distance;
		public Map<String, PluginCurrencySection> cost;
		
		public AmplificationForgeSectionData() {}
		
		public AmplificationForgeSectionData(
			double chance,
			double rate,
			int distance,
			Map<String, PluginCurrencySection> cost
		) {
			this.chance = chance;
			this.rate = rate;
			this.distance = distance;
			this.cost = cost;
		}
	}
}
