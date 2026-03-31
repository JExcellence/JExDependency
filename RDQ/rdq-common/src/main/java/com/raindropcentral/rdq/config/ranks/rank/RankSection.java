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

import com.raindropcentral.rdq.config.requirement.BaseRequirementSection;
import com.raindropcentral.rplatform.config.icon.IconSection;
import com.raindropcentral.rdq.config.utility.RewardSection;
import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.configmapper.sections.CSIgnore;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a configuration section for a rank within a rank tree.
 * Contains all properties, requirements, rewards, and connections for a specific rank.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
@CSAlways
public class RankSection extends AConfigSection {
    
    /** The key for the display name of the rank (for localization). */
    private String displayNameKey;
    
    /** The key for the description/lore of the rank (for localization). */
    private String descriptionKey;
    
    /** The LuckPerms group associated with this rank. */
    private String luckPermsGroup;
    
    /** The key for the prefix of the rank (for localization). */
    private String prefixKey;
    
    /** The key for the suffix of the rank (for localization). */
    private String suffixKey;
    
    /** The tier/level of the rank within the tree. */
    private Integer tier;
    
    /** The weight of the rank, used for ordering or comparison. */
    private Integer weight;
    
    /** The icon representing this rank. */
    private IconSection icon;
    
    /** List of previous rank names (prerequisites). */
    private List<String> previousRanks;
    
    /** List of next rank names (progression). */
    private List<String> nextRanks;
    
    /** Map of requirement keys to their configuration sections. */
    private Map<String, BaseRequirementSection> requirements;
    
    /** Map of reward keys to their configuration sections. */
    private Map<String, RewardSection> rewards;
    
    /** Whether this rank is the initial rank in the tree. */
    private Boolean isInitialRank;
    
    /** Whether this rank is the final rank in the tree. */
    private Boolean isFinalRank;
    
    /** Whether all rank trees must be completed to access this rank. */
    private Boolean requiresAllRankTreesToBeDone;
    
    /** Whether this rank is enabled. */
    private Boolean isEnabled;
    
    /** List of connections to ranks in other trees. */
    private List<String> crossRankTreeConnections;
    
    /** The name of the rank tree this rank belongs to. */
    @CSIgnore
    private String rankTreeName;
    
    /** The name of this rank. */
    @CSIgnore
    private String rankName;
    
    /**
     * Constructs a new RankSection with the given evaluation environment builder.
     *
     * @param evaluationEnvironmentBuilder the evaluation environment builder
     */
    public RankSection(
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
            this.rankTreeName != null &&
            this.rankName != null
        ) {
            if (
                this.displayNameKey == null
            ) {
                this.displayNameKey = "rank." + this.rankTreeName + "." + this.rankName + ".name";
            }
            if (
                this.descriptionKey == null
            ) {
                this.descriptionKey = "rank." + this.rankTreeName + "." + this.rankName + ".lore";
            }
            if (
                this.prefixKey == null
            ) {
                this.prefixKey = "rank." + this.rankTreeName + "." + this.rankName + ".prefix";
            }
            if (
                this.suffixKey == null
            ) {
                this.suffixKey = "rank." + this.rankTreeName + "." + this.rankName + ".suffix";
            }
        }
    }
    
    /**
     * Gets the display name key for this rank.
     *
     * @return the display name key, or "not_defined" if not set
     */
    public String getDisplayNameKey() {
        return
            this.displayNameKey == null ?
            "not_defined" :
            this.displayNameKey;
    }
    
    /**
     * Gets the description key for this rank.
     *
     * @return the description key, or "not_defined" if not set
     */
    public String getDescriptionKey() {
        return
            this.descriptionKey == null ?
            "not_defined" :
            this.descriptionKey;
    }
    
    /**
     * Gets the LuckPerms group associated with this rank.
     *
     * @return the LuckPerms group, or "not_defined" if not set
     */
    public String getLuckPermsGroup() {
        return
            this.luckPermsGroup == null ?
            "not_defined" :
            this.luckPermsGroup;
    }
    
    /**
     * Gets the prefix key for this rank.
     *
     * @return the prefix key, or an empty string if not set
     */
    public String getPrefixKey() {
        return
            this.prefixKey == null ?
            "" :
            this.prefixKey;
    }
    
    /**
     * Gets the suffix key for this rank.
     *
     * @return the suffix key, or an empty string if not set
     */
    public String getSuffixKey() {
        return
            this.suffixKey == null ?
            "" :
            this.suffixKey;
    }
    
    /**
     * Gets the tier/level of this rank.
     *
     * @return the tier, or 0 if not set
     */
    public Integer getTier() {
        return
            this.tier == null ?
            0 :
            this.tier;
    }
    
    /**
     * Gets the weight of this rank.
     *
     * @return the weight, or 0 if not set
     */
    public Integer getWeight() {
        return
            this.weight == null ?
            0 :
            this.weight;
    }
    
    /**
     * Gets the icon section for this rank.
     *
     * @return the icon section, or a new default IconSection if not set
     */
    public IconSection getIcon() {
        return
            this.icon == null ?
            new IconSection(new EvaluationEnvironmentBuilder()) :
            this.icon;
    }
    
    /**
     * Gets the list of previous ranks (prerequisites).
     *
     * @return the list of previous rank names, or an empty list if not set
     */
    public List<String> getPreviousRanks() {
        return
            this.previousRanks == null ?
            new ArrayList<>() :
            this.previousRanks;
    }
    
    /**
     * Gets the list of next ranks (progression).
     *
     * @return the list of next rank names, or an empty list if not set
     */
    public List<String> getNextRanks() {
        return
            this.nextRanks == null ?
            new ArrayList<>() :
            this.nextRanks;
    }
    
    /**
     * Gets the requirements for this rank.
     *
     * @return the map of requirement keys to sections, or an empty map if not set
     */
    public Map<String, BaseRequirementSection> getRequirements() {
        return
            this.requirements == null ?
            new HashMap<>() :
            this.requirements;
    }
    
    /**
     * Gets the rewards for this rank.
     *
     * @return the map of reward keys to sections, or an empty map if not set
     */
    public Map<String, RewardSection> getRewards() {
        return
            this.rewards == null ?
            new HashMap<>() :
            this.rewards;
    }
    
    /**
     * Checks if this rank is the initial rank in the tree.
     *
     * @return true if this is the initial rank, false otherwise
     */
    public Boolean getInitialRank() {
        return
            this.isInitialRank != null &&
            ! this.getFinalRank();
    }
    
    /**
     * Checks if this rank is the final rank in the tree.
     *
     * @return true if this is the final rank, false otherwise
     */
    public Boolean getFinalRank() {
        return
            this.isFinalRank != null &&
            this.isFinalRank;
    }
    
    /**
     * Checks if all rank trees must be completed to access this rank.
     *
     * @return true if all rank trees are required, false otherwise
     */
    public Boolean getRequiresAllRankTreesToBeDone() {
        return
            this.requiresAllRankTreesToBeDone != null &&
            this.requiresAllRankTreesToBeDone;
    }
    
    /**
     * Checks if this rank is enabled.
     *
     * @return true if enabled, false otherwise
     */
    public Boolean getEnabled() {
        return
            this.isEnabled != null &&
            this.isEnabled;
    }
    
    /**
     * Gets the list of cross-rank tree connections.
     *
     * @return the list of cross-rank tree connections, or an empty list if not set
     */
    public List<String> getCrossRankTreeConnections() {
        return
            this.crossRankTreeConnections == null ?
            new ArrayList<>() :
            this.crossRankTreeConnections;
    }
    
    /**
     * Gets the name of the rank tree this rank belongs to.
     *
     * @return the rank tree name, or a generated name if not set
     */
    public String getRankTreeName() {
        return
            this.rankTreeName == null ?
            "not_defined_" + UUID.randomUUID() :
            this.rankTreeName;
    }
    
    /**
     * Sets the name of the rank tree this rank belongs to.
     *
     * @param rankTreeName the rank tree name
     */
    public void setRankTreeName(final String rankTreeName) {
        this.rankTreeName = rankTreeName;
    }
    
    /**
     * Gets the name of this rank.
     *
     * @return the rank name, or a generated name if not set
     */
    public String getRankName() {
        return
            this.rankName == null ?
            "not_defined_" + UUID.randomUUID() :
            this.rankName;
    }
    
    /**
     * Sets the name of this rank.
     *
     * @param rankName the rank name
     */
    public void setRankName(final String rankName) {
        this.rankName = rankName;
    }
    
}
