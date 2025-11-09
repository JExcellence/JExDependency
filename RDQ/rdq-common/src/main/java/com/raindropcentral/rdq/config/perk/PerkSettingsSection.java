package com.raindropcentral.rdq.config.perk;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.raindropcentral.rdq.config.item.IconSection;
import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.configmapper.sections.CSIgnore;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration section that encapsulates general settings for an RDQ perk, including
 * localization keys, visibility, and metadata.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@CSAlways
public class PerkSettingsSection extends AConfigSection {
	
	private IconSection icon;
	
	/**
	 * The localization key for the display name of the perk.
	 * If null, defaults to "perk.{perkId}.name".
	 */
	private String displayNameKey;
	
	/**
	 * The localization key for the description/lore of the perk.
	 * If null, defaults to "perk.{perkId}.lore".
	 */
	private String descriptionKey;
	
	private Integer priority;
	
	private Integer maxConcurrentUsers;
	
	private Boolean requiresOwnedArea;
	
	private Boolean isEnabled;
	
	private Map<String, Object> metadata;
	
	/**
	 * The name of this perk (set by the factory).
	 */
	@CSIgnore
	private String perkId;
	
        /**
         * Default constructor for Jackson deserialization.
         */
        @JsonCreator
        public PerkSettingsSection() {
                super(new EvaluationEnvironmentBuilder());
        }

        /**
         * Creates a new instance using the provided evaluation environment builder.
         *
         * @param evaluationEnvironmentBuilder the builder used to resolve dynamic configuration values
         */
        public PerkSettingsSection(
                final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder
        ) {
                super(evaluationEnvironmentBuilder);
        }
	
	/**
	 * Called after parsing the configuration fields. Sets default localization keys if not provided.
	 *
	 * @param fields the list of fields parsed
	 * @throws Exception if an error occurs during post-processing
	 */
	@Override
        public void afterParsing(final List<Field> fields) throws Exception {
                super.afterParsing(fields);
                this.applyDefaultLocalizationKeys();
        }

        /**
         * Sets the perk ID for this perk settings section.
         *
         * @param perkId the perk identifier
         */
        public void setPerkId(final String perkId) {
                this.perkId = perkId;
                this.applyDefaultLocalizationKeys();
        }
	
	/**
	 * Gets the perk ID for this perk settings section.
	 *
	 * @return the perk identifier
	 */
	public String getPerkId() {
		return this.perkId;
	}
	
        /**
         * Resolves the icon configuration for the perk.
         *
         * @return the configured icon, or a default instance when none is defined
         */
        public IconSection getIcon() {
                return
                        this.icon == null ?
                        new IconSection(new EvaluationEnvironmentBuilder()) :
			this.icon;
	}
	
        /**
         * Provides the localization key that resolves the perk's display name.
         *
         * @return the display name localization key or {@code "not_defined"} when no key is set
         */
        public String getDisplayNameKey() {
                return
                        this.displayNameKey == null ?
                        "not_defined" :
                        this.displayNameKey;
        }

        /**
         * Provides the localization key that resolves the perk's lore or description text.
         *
         * @return the description localization key or {@code "not_defined"} when no key is set
         */
        public String getDescriptionKey() {
                return
                        this.descriptionKey == null ?
                        "not_defined" :
			this.descriptionKey;
	}
	
        /**
         * Returns the configured ordering priority for the perk.
         *
         * @return the priority value or {@code 0} when no priority has been defined
         */
        public Integer getPriority() {
                return
                        this.priority == null ?
                        0 :
			this.priority;
	}
	
        /**
         * Provides the limit on how many users may enable the perk simultaneously.
         *
         * @return the concurrency limit or {@code null} when unlimited
         */
        public Integer getMaxConcurrentUsers() {
                return
                        this.maxConcurrentUsers == null ?
                        -1 :
			this.maxConcurrentUsers;
	}
	
        /**
         * Determines whether the perk requires the user to own an area to activate it.
         *
         * @return {@code true} when the perk requires ownership, otherwise {@code false}
         */
        public Boolean getRequiresOwnedArea() {
                return
                        this.requiresOwnedArea != null &&
                        this.requiresOwnedArea;
        }

        /**
         * Indicates whether the perk is enabled in configuration.
         *
         * @return {@code true} when the perk is explicitly enabled or not specified, otherwise {@code false}
         */
        public Boolean getEnabled() {
                return
                        this.isEnabled == null ||
                        this.isEnabled;
        }

        /**
         * Resolves additional configuration metadata for the perk.
         *
         * @return a modifiable map of metadata values, or an empty map when none are configured
         */
        public Map<String, Object> getMetadata() {
                if (this.metadata == null) {
                        this.metadata = new HashMap<>();
                }
                return this.metadata;
        }

        private void applyDefaultLocalizationKeys() {
                final String effectiveId = this.resolvePerkId();
                if (effectiveId == null || effectiveId.isBlank()) {
                        return;
                }

                if (this.perkId == null || this.perkId.isBlank()) {
                        this.perkId = effectiveId;
                }

                if (this.displayNameKey == null || "not_defined".equalsIgnoreCase(this.displayNameKey)) {
                        this.displayNameKey = "perk." + effectiveId + ".name";
                }

                if (this.descriptionKey == null || "not_defined".equalsIgnoreCase(this.descriptionKey)) {
                        this.descriptionKey = "perk." + effectiveId + ".lore";
                }

                if (this.icon != null) {
                        if (this.icon.getDisplayNameKey() == null || this.icon.getDisplayNameKey().equals("not_defined")) {
                                this.icon.setDisplayNameKey("perk." + effectiveId + ".name");
                        }
                        if (this.icon.getDescriptionKey() == null || this.icon.getDescriptionKey().equals("not_defined")) {
                                this.icon.setDescriptionKey("perk." + effectiveId + ".lore");
                        }
                }
        }

        private String resolvePerkId() {
                if (this.perkId != null && !this.perkId.isBlank()) {
                        return this.perkId;
                }

                if (this.metadata != null) {
                        final Object metadataId = this.metadata.get("id");
                        if (metadataId instanceof String id && !id.isBlank()) {
                                return id;
                        }

                        final Object identifier = this.metadata.get("identifier");
                        if (identifier instanceof String id && !id.isBlank()) {
                                return id;
                        }
                }

                return null;
        }
}