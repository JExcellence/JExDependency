package com.raindropcentral.rdq.database.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raindropcentral.rdq.config.perks.PluginCurrencySection;
import com.raindropcentral.rdq.config.perks.sections.forge.AtomicForgeSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Converter(autoApply = true)
public class AtomicForgeSectionConverter implements AttributeConverter<AtomicForgeSection, String> {
	
	private static final Logger       LOGGER        = Logger.getLogger(AtomicForgeSectionConverter.class.getName());
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private final ConverterTool       converterTool = new ConverterTool();
	
	@Override
	public String convertToDatabaseColumn(final AtomicForgeSection atomicForgeSection) {
		if (atomicForgeSection == null) {
			return null;
		}
		
		try {
			final AtomicForgeSectionData data = new AtomicForgeSectionData(
				atomicForgeSection.getAtomicAcceleratorSection().getRate(),
				atomicForgeSection.getAtomicEconomizerSection().getCurrencySections(),
				atomicForgeSection.getAtomicInvestorSection().getTimer(),
				atomicForgeSection.getAtomicInvestorSection().getCurrencySections()
			);
			
			return OBJECT_MAPPER.writeValueAsString(data);
		} catch (final JsonProcessingException e) {
			LOGGER.log(Level.SEVERE, "Failed to convert AtomicForgeSectionData to JSON", e);
			throw new RuntimeException("Failed to serialize AtomicForgeSectionData", e);
		}
	}
	
	@Override
	public AtomicForgeSection convertToEntityAttribute(
		@Nullable final String jsonString
	) {
		if (jsonString == null || jsonString.trim().isEmpty()) {
			return null;
		}
		
		try {
			final JsonNode jsonNode = OBJECT_MAPPER.readTree(jsonString);
			final AtomicForgeSectionData data = OBJECT_MAPPER.treeToValue(jsonNode, AtomicForgeSectionData.class);
			final AtomicForgeSection atomicSection = new AtomicForgeSection(new EvaluationEnvironmentBuilder());
			
			converterTool.setPrivateField(atomicSection, "rate", data.rate, LOGGER);
			converterTool.setPrivateField(atomicSection, "timer", data.timer, LOGGER);
			
			//TODO help with converting PluginCurrencySection
			
			return atomicSection;
		} catch (final Exception e) {
			LOGGER.log(Level.SEVERE, "Failed to convert JSON to IconSection: " + jsonString, e);
			throw new RuntimeException("Failed to deserialize IconSection", e);
		}
	}
	
	
	
	private static class AtomicForgeSectionData {
		public double                             rate;
		public Map<String, PluginCurrencySection> economizer_currencySections;
		public int                                timer;
		public Map<String, PluginCurrencySection> investor_currencySections;
		
		public AtomicForgeSectionData() {}
		
		public AtomicForgeSectionData(
			double rate,
			Map<String, PluginCurrencySection> economizer_currencySections,
			int timer,
			Map<String, PluginCurrencySection> investor_currencySections
		) {
			this.rate = rate;
			this.economizer_currencySections = economizer_currencySections;
			this.timer = timer;
			this.investor_currencySections = investor_currencySections;
		}
	}
}
