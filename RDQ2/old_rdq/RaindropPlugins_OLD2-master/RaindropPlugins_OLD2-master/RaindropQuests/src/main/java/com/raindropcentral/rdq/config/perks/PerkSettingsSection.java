package com.raindropcentral.rdq.config.perks;

import com.raindropcentral.rdq.config.utility.IconSection;
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
		
		if (
			this.perkId != null
		) {
			if (
				this.displayNameKey == null
			) {
				this.displayNameKey = "perk." + this.perkId + ".name";
			}
			
			if (
				this.descriptionKey == null
			) {
				this.descriptionKey = "perk." + this.perkId + ".lore";
			}
			
			if (
				this.icon != null
			) {
				if (
					this.icon.getDisplayNameKey() == null ||
					this.icon.getDisplayNameKey().equals("not_defined")
				) {
					this.icon.setDisplayNameKey("perk." + this.perkId + ".name");
				}
				if (
					this.icon.getDescriptionKey() == null ||
					this.icon.getDescriptionKey().equals("not_defined")
				) {
					this.icon.setDescriptionKey("perk." + this.perkId + ".lore");
				}
			}
		}
	}
	
	/**
	 * Sets the perk ID for this perk settings section.
	 *
	 * @param perkId the perk identifier
	 */
	public void setPerkId(final String perkId) {
		this.perkId = perkId;
	}
	
	/**
	 * Gets the perk ID for this perk settings section.
	 *
	 * @return the perk identifier
	 */
	public String getPerkId() {
		return this.perkId;
	}
	
	public IconSection getIcon() {
		return
			this.icon == null ?
			new IconSection(new EvaluationEnvironmentBuilder()) :
			this.icon;
	}
	
	public String getDisplayNameKey() {
		return
			this.displayNameKey == null ?
			"not_defined" :
			this.displayNameKey;
	}
	
	public String getDescriptionKey() {
		return
			this.descriptionKey == null ?
			"not_defined" :
			this.descriptionKey;
	}
	
	public Integer getPriority() {
		return
			this.priority == null ?
			0 :
			this.priority;
	}
	
	public Integer getMaxConcurrentUsers() {
		return
			this.maxConcurrentUsers == null ?
			null :
			this.maxConcurrentUsers;
	}
	
	public Boolean getRequiresOwnedArea() {
		return
			this.requiresOwnedArea != null &&
			this.requiresOwnedArea;
	}
	
	public Boolean getEnabled() {
		return
			this.isEnabled == null ||
			this.isEnabled;
	}
	
	public Map<String, Object> getMetadata() {
		return
			this.metadata == null ?
			new HashMap<>() :
			this.metadata;
	}
}