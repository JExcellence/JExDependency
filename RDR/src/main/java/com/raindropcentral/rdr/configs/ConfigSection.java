package com.raindropcentral.rdr.configs;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

@CSAlways
@SuppressWarnings("unused")
public class ConfigSection extends AConfigSection {
	
	public ConfigSection(EvaluationEnvironmentBuilder baseEnvironment) {
		super(baseEnvironment);
	}
	
}
