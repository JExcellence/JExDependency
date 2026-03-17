/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rdq.config.ranks.rank;

import com.raindropcentral.rdq.config.utility.IconSection;
import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

/**
 * Configuration section for default rank settings.
 *
 * <p>This section defines the default rank and rank tree that players receive
 * when they first join the server, before they select their progression path.
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
	
	/**
	 * Executes DefaultRankSection.
	 */
	public DefaultRankSection(final EvaluationEnvironmentBuilder baseEnvironment) {
		super(baseEnvironment);
	}
	
	/**
	 * Gets defaultRankTreeIdentifier.
	 */
	public String getDefaultRankTreeIdentifier() {
		return this.defaultRankTreeIdentifier == null ?
		       "default_unselected" :
		       this.defaultRankTreeIdentifier;
	}
	
	/**
	 * Gets defaultRankIdentifier.
	 */
	public String getDefaultRankIdentifier() {
		return this.defaultRankIdentifier == null ?
		       "unselected_rank" :
		       this.defaultRankIdentifier;
	}
	
	/**
	 * Gets visible.
	 */
	public Boolean getVisible() {
		return this.isVisible == null || this.isVisible;
	}
	
	/**
	 * Gets selectable.
	 */
	public Boolean getSelectable() {
		return this.isSelectable != null && this.isSelectable;
	}
	
	/**
	 * Gets displayNameKey.
	 */
	public String getDisplayNameKey() {
		return this.displayNameKey == null ?
		       "rank.default.unselected.name" :
		       this.displayNameKey;
	}
	
	/**
	 * Gets descriptionKey.
	 */
	public String getDescriptionKey() {
		return this.descriptionKey == null ?
		       "rank.default.unselected.lore" :
		       this.descriptionKey;
	}
	
	/**
	 * Gets icon.
	 */
	public IconSection getIcon() {
		return this.icon == null ?
		       new IconSection(new EvaluationEnvironmentBuilder()) :
		       this.icon;
	}
	
	/**
	 * Gets tier.
	 */
	public Integer getTier() {
		return this.tier == null ? 1 : this.tier;
	}
	
	/**
	 * Gets weight.
	 */
	public Integer getWeight() {
		return this.weight == null ? 0 : this.weight;
	}
	
	/**
	 * Gets luckPermsGroup.
	 */
	public String getLuckPermsGroup() {
		return this.luckPermsGroup == null ? "default" : this.luckPermsGroup;
	}
	
	/**
	 * Gets prefixKey.
	 */
	public String getPrefixKey() {
		return this.prefixKey == null ? "" : this.prefixKey;
	}
	
	/**
	 * Gets suffixKey.
	 */
	public String getSuffixKey() {
		return this.suffixKey == null ? "" : this.suffixKey;
	}
	
	/**
	 * Gets startingRank.
	 */
	public Boolean getStartingRank() {
		return this.isInitialRank != null && this.isInitialRank;
	}
	
	/**
	 * Gets enabled.
	 */
	public Boolean getEnabled() {
		return this.enabled == null || this.enabled;
	}
}
