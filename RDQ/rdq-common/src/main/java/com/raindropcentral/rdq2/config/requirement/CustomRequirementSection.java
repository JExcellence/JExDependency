/*
package com.raindropcentral.rdq2.config.requirement;

import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.HashMap;
import java.util.Map;

*/
/**
 * Configuration section for custom requirements.
 * <p>
 * This section consolidates all configuration properties used to evaluate
 * {@code CustomRequirement} instances, including executable scripts, backing
 * Java classes, arbitrary data payloads, and cache settings. Each getter merges
 * canonical and legacy field names so YAML definitions remain backward
 * compatible across configuration revisions.
 * </p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 *//*

@CSAlways
public class CustomRequirementSection extends BaseRequirementSection {
	
	// ~~~ CUSTOM-SPECIFIC PROPERTIES ~~~
	
        */
/**
         * Custom script code for requirement evaluation.
         *
         * <p>YAML key: {@code customScript}</p>
         *//*

        private String customScript;

        */
/**
         * Alternative script field name.
         *
         * <p>YAML key: {@code script}</p>
         *//*

        private String script;

        */
/**
         * Script language identifier (e.g., "javascript", "groovy", "python").
         *
         * <p>YAML key: {@code scriptLanguage}</p>
         *//*

        private String scriptLanguage;

        */
/**
         * Alternative script language field name.
         *
         * <p>YAML key: {@code language}</p>
         *//*

        private String language;

        */
/**
         * Custom data for the requirement.
         *
         * <p>YAML key: {@code customData}</p>
         *//*

        private Map<String, Object> customData;

        */
/**
         * Alternative custom data field name.
         *
         * <p>YAML key: {@code data}</p>
         *//*

        private Map<String, Object> data;

        */
/**
         * Custom class name for requirement implementation.
         *
         * <p>YAML key: {@code customClass}</p>
         *//*

        private String customClass;

        */
/**
         * Alternative class field name.
         *
         * <p>YAML key: {@code className}</p>
         *//*

        private String className;

        */
/**
         * Parameters for custom class or script.
         *
         * <p>YAML key: {@code parameters}</p>
         *//*

        private Map<String, String> parameters;

        */
/**
         * Alternative parameters field name.
         *
         * <p>YAML key: {@code params}</p>
         *//*

        private Map<String, String> params;

        */
/**
         * Whether this requirement should consume resources when completed.
         *
         * <p>YAML key: {@code consumeOnComplete}</p>
         *//*

        private Boolean consumeOnComplete;

        */
/**
         * Custom configuration section for complex custom requirements.
         *
         * <p>YAML key: {@code customConfig}</p>
         *//*

        private Map<String, Object> customConfig;

        */
/**
         * Whether to cache the result of custom evaluation.
         *
         * <p>YAML key: {@code cacheResult}</p>
         *//*

        private Boolean cacheResult;

        */
/**
         * Cache duration in seconds for custom evaluation results.
         *
         * <p>YAML key: {@code cacheDuration}</p>
         *//*

        private Long cacheDuration;

        */
/**
         * Constructs a new CustomRequirementSection.
         *
         * @param evaluationEnvironmentBuilder the builder that produces the
         *                                      evaluation environment used when
         *                                      interpreting custom requirement
         *                                      scripts
         *//*

        public CustomRequirementSection(
                final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder
        ) {
                super(evaluationEnvironmentBuilder);
        }

        */
/**
         * Resolves the executable script backing the requirement.
         *
         * <p>The lookup order prefers {@code customScript}, then {@code script},
         * and finally returns an empty string when no script is defined.</p>
         *
         * @return the script code to execute, or an empty string when absent
         *//*

        public String getCustomScript() {
                if (this.customScript != null) {
                        return this.customScript;
                }
		if (this.script != null) {
			return this.script;
		}
		return "";
	}
	
        */
/**
         * Resolves the configured scripting language for evaluation.
         *
         * <p>The lookup order prefers {@code scriptLanguage}, then
         * {@code language}, and defaults to {@code javascript} to maintain
         * backwards compatibility.</p>
         *
         * @return the language identifier to pass to the script engine
         *//*

        public String getScriptLanguage() {
                if (this.scriptLanguage != null) {
                        return this.scriptLanguage;
                }
                if (this.language != null) {
			return this.language;
		}
		return "javascript";
	}
	
        */
/**
         * Aggregates all supplied custom data maps.
         *
         * <p>The resulting {@link HashMap} combines entries from
         * {@code customData} and {@code data}. Later entries override earlier
         * ones when duplicate keys exist.</p>
         *
         * @return a mutable map containing the merged custom data; empty when no
         *         data is provided
         *//*

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
	
        */
/**
         * Resolves the fully qualified class responsible for the requirement.
         *
         * <p>The lookup order prefers {@code customClass}, then {@code className}
         * and returns an empty string when no explicit class is configured.</p>
         *
         * @return the class name used for instantiation, or an empty string when
         *         unspecified
         *//*

        public String getCustomClass() {
                if (this.customClass != null) {
                        return this.customClass;
                }
                if (this.className != null) {
			return this.className;
		}
		return "";
	}
	
        */
/**
         * Aggregates all parameters passed to the custom class or script.
         *
         * <p>The resulting map merges {@code parameters} with {@code params},
         * allowing legacy aliases to override base values when key collisions
         * occur.</p>
         *
         * @return a mutable map containing the merged parameters; empty when
         *         none are defined
         *//*

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
	
        */
/**
         * Indicates whether the requirement should consume tracked resources on
         * completion.
         *
         * @return {@code true} when consumption is enabled, otherwise
         *         {@code false}
         *//*

        public Boolean getConsumeOnComplete() {
                return this.consumeOnComplete != null ? this.consumeOnComplete : false;
        }

        */
/**
         * Retrieves additional nested configuration supplied for bespoke
         * requirements.
         *
         * @return a mutable map of custom configuration values, or an empty map
         *         when no configuration is provided
         *//*

        public Map<String, Object> getCustomConfig() {
                return this.customConfig != null ? this.customConfig : new HashMap<>();
        }

        */
/**
         * Determines whether the evaluation result should be cached for future
         * checks.
         *
         * @return {@code true} when caching is enabled, otherwise {@code false}
         *//*

        public Boolean getCacheResult() {
                return this.cacheResult != null ? this.cacheResult : false;
        }

        */
/**
         * Provides the cache duration for stored evaluation results.
         *
         * @return the duration, in seconds, defaulting to {@code 300} (five
         *         minutes) when unspecified
         *//*

        public Long getCacheDuration() {
                return this.cacheDuration != null ? this.cacheDuration : 300L;
        }
}*/
