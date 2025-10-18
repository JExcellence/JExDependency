package com.raindropcentral.rdq.config.ranks.rank;


import com.raindropcentral.rdq.config.item.IconSection;
import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

/**
 * Configuration section for default rank settings.
 * <p>
 * This section defines the default rank and rank tree that players receive
 * when they first join the server, before they select their progression path.
 * </p>
 */
public class DefaultRankSection extends AConfigSection {
	
	@CSAlways
	private String defaultRankTreeIdentifier;
	
	@CSAlways
	private String defaultRankIdentifier;
	
	@CSAlways
	private Boolean isVisible;
	
	@CSAlways
	private Boolean isSelectable;
	
	@CSAlways
	private String displayNameKey;
	
	@CSAlways
	private String descriptionKey;
	
	@CSAlways
	private IconSection icon;
	
	@CSAlways
	private Integer tier;
	
	@CSAlways
	private Integer weight;
	
	@CSAlways
	private String luckPermsGroup;
	
	@CSAlways
	private String prefixKey;
	
	@CSAlways
	private String suffixKey;
	
	@CSAlways
	private Boolean isInitialRank;
	
	@CSAlways
	private Boolean enabled;
	
	public DefaultRankSection(final EvaluationEnvironmentBuilder baseEnvironment) {
		super(baseEnvironment);
	}
	
	public String getDefaultRankTreeIdentifier() {
		return this.defaultRankTreeIdentifier == null ?
		       "default_unselected" :
		       this.defaultRankTreeIdentifier;
	}
	
	public String getDefaultRankIdentifier() {
		return this.defaultRankIdentifier == null ?
		       "unselected_rank" :
		       this.defaultRankIdentifier;
	}
	
	public Boolean getVisible() {
		return this.isVisible == null || this.isVisible;
	}
	
	public Boolean getSelectable() {
		return this.isSelectable != null && this.isSelectable;
	}
	
	public String getDisplayNameKey() {
		return this.displayNameKey == null ?
		       "rank.default.unselected.name" :
		       this.displayNameKey;
	}
	
	public String getDescriptionKey() {
		return this.descriptionKey == null ?
		       "rank.default.unselected.lore" :
		       this.descriptionKey;
	}
	
	public IconSection getIcon() {
		return this.icon == null ?
		       new IconSection(new EvaluationEnvironmentBuilder()) :
		       this.icon;
	}
	
	public Integer getTier() {
		return this.tier == null ? 1 : this.tier;
	}
	
	public Integer getWeight() {
		return this.weight == null ? 0 : this.weight;
	}
	
	public String getLuckPermsGroup() {
		return this.luckPermsGroup == null ? "default" : this.luckPermsGroup;
	}
	
	public String getPrefixKey() {
		return this.prefixKey == null ? "" : this.prefixKey;
	}
	
	public String getSuffixKey() {
		return this.suffixKey == null ? "" : this.suffixKey;
	}
	
	public Boolean getStartingRank() {
		return this.isInitialRank != null && this.isInitialRank;
	}
	
	public Boolean getEnabled() {
		return this.enabled == null || this.enabled;
	}
}