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

@CSAlways
public class PerkSettingsSection extends AConfigSection {
	
	private IconSection icon;
	
	private String displayNameKey;
	private String descriptionKey;
	private Integer priority;
	private Integer maxConcurrentUsers;
	private Boolean requiresOwnedArea;
	private Boolean isEnabled;
	private Map<String, Object> metadata;
	
	@CSIgnore
	private String perkId;
	
        @JsonCreator
        public PerkSettingsSection() {
                super(new EvaluationEnvironmentBuilder());
        }

        public PerkSettingsSection(
                EvaluationEnvironmentBuilder evaluationEnvironmentBuilder
        ) {
                super(evaluationEnvironmentBuilder);
        }
	
	@Override
        public void afterParsing(List<Field> fields) throws Exception {
                super.afterParsing(fields);
                this.applyDefaultLocalizationKeys();
        }

        public void setPerkId(String perkId) {
                this.perkId = perkId;
                this.applyDefaultLocalizationKeys();
        }
	
	public String getPerkId() {
		return this.perkId;
	}
	
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