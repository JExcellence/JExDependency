package com.raindropcentral.rdq.config.requirement;

import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration section for custom requirements.
 * <p>
 * This section handles all configuration options specific to CustomRequirement,
 * including custom scripts, custom classes, parameters, and custom data handling.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
@CSAlways
public class CustomRequirementSection extends BaseRequirementSection {
	
	// ~~~ CUSTOM-SPECIFIC PROPERTIES ~~~
	
	/**
	 * Custom script code for requirement evaluation.
	 * YAML key: "customScript"
	 */
	private String customScript;
	
	/**
	 * Alternative script field name.
	 * YAML key: "script"
	 */
	private String script;
	
	/**
	 * Script language identifier (e.g., "javascript", "groovy", "python").
	 * YAML key: "scriptLanguage"
	 */
	private String scriptLanguage;
	
	/**
	 * Alternative script language field name.
	 * YAML key: "language"
	 */
	private String language;
	
	/**
	 * Custom data for the requirement.
	 * YAML key: "customData"
	 */
	private Map<String, Object> customData;
	
	/**
	 * Alternative custom data field name.
	 * YAML key: "data"
	 */
	private Map<String, Object> data;
	
	/**
	 * Custom class name for requirement implementation.
	 * YAML key: "customClass"
	 */
	private String customClass;
	
	/**
	 * Alternative class field name.
	 * YAML key: "className"
	 */
	private String className;
	
	/**
	 * Parameters for custom class or script.
	 * YAML key: "parameters"
	 */
	private Map<String, String> parameters;
	
	/**
	 * Alternative parameters field name.
	 * YAML key: "params"
	 */
	private Map<String, String> params;
	
	/**
	 * Whether this requirement should consume resources when completed.
	 * YAML key: "consumeOnComplete"
	 */
	private Boolean consumeOnComplete;
	
	/**
	 * Custom configuration section for complex custom requirements.
	 * YAML key: "customConfig"
	 */
	private Map<String, Object> customConfig;
	
	/**
	 * Whether to cache the result of custom evaluation.
	 * YAML key: "cacheResult"
	 */
	private Boolean cacheResult;
	
	/**
	 * Cache duration in seconds for custom evaluation results.
	 * YAML key: "cacheDuration"
	 */
	private Long cacheDuration;
	
	/**
	 * Constructs a new CustomRequirementSection.
	 *
	 * @param evaluationEnvironmentBuilder the evaluation environment builder
	 */
	public CustomRequirementSection(
		final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder
	) {
		super(evaluationEnvironmentBuilder);
	}
	
	/**
	 * Gets the custom script, trying multiple field names.
	 *
	 * @return the custom script code
	 */
	public String getCustomScript() {
		if (this.customScript != null) {
			return this.customScript;
		}
		if (this.script != null) {
			return this.script;
		}
		return "";
	}
	
	/**
	 * Gets the script language, trying multiple field names.
	 *
	 * @return the script language identifier, defaulting to "javascript"
	 */
	public String getScriptLanguage() {
		if (this.scriptLanguage != null) {
			return this.scriptLanguage;
		}
		if (this.language != null) {
			return this.language;
		}
		return "javascript";
	}
	
	/**
	 * Gets the complete custom data from all sources.
	 *
	 * @return combined map of all custom data
	 */
	public Map<String, Object> getCustomData() {
		Map<String, Object> dataMap = new HashMap<>();
		
		// Add data from customData map
		if (this.customData != null) {
			dataMap.putAll(this.customData);
		}
		
		// Add data from alternative data map
		if (this.data != null) {
			dataMap.putAll(this.data);
		}
		
		return dataMap;
	}
	
	/**
	 * Gets the custom class name, trying multiple field names.
	 *
	 * @return the custom class name
	 */
	public String getCustomClass() {
		if (this.customClass != null) {
			return this.customClass;
		}
		if (this.className != null) {
			return this.className;
		}
		return "";
	}
	
	/**
	 * Gets the complete parameters from all sources.
	 *
	 * @return combined map of all parameters
	 */
	public Map<String, String> getParameters() {
		Map<String, String> paramMap = new HashMap<>();
		
		// Add parameters from parameters map
		if (this.parameters != null) {
			paramMap.putAll(this.parameters);
		}
		
		// Add parameters from alternative params map
		if (this.params != null) {
			paramMap.putAll(this.params);
		}
		
		return paramMap;
	}
	
	/**
	 * Gets whether resources should be consumed on completion.
	 *
	 * @return true if resources should be consumed, false otherwise
	 */
	public Boolean getConsumeOnComplete() {
		return this.consumeOnComplete != null ? this.consumeOnComplete : false;
	}
	
	/**
	 * Gets the custom configuration.
	 *
	 * @return the custom configuration map
	 */
	public Map<String, Object> getCustomConfig() {
		return this.customConfig != null ? this.customConfig : new HashMap<>();
	}
	
	/**
	 * Gets whether to cache evaluation results.
	 *
	 * @return true if results should be cached, false otherwise
	 */
	public Boolean getCacheResult() {
		return this.cacheResult != null ? this.cacheResult : false;
	}
	
	/**
	 * Gets the cache duration in seconds.
	 *
	 * @return the cache duration, defaulting to 300 seconds (5 minutes)
	 */
	public Long getCacheDuration() {
		return this.cacheDuration != null ? this.cacheDuration : 300L;
	}
}