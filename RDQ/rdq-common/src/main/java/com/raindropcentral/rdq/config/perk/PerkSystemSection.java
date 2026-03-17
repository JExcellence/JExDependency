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

package com.raindropcentral.rdq.config.perk;

import com.raindropcentral.rdq.config.ranks.system.NotificationTypeSection;
import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.configmapper.sections.CSIgnore;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.lang.reflect.Field;
import java.util.List;
import java.util.logging.Logger;

/**
 * Configuration section for global perk system settings.
 *
 * <p>This section handles system-wide perk configuration including enable/disable,
 * player limits, cooldown multipliers, UI settings, notifications, and integration settings.
 * Default values are provided for all fields if not set.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
@CSAlways
public class PerkSystemSection extends AConfigSection {
	
	@CSIgnore
	private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
	
	// ==================== System Settings ====================
	
	/**
	 * Whether the perk system is enabled globally.
	 * If null, defaults to true.
	 */
	private Boolean enabled;
	
	/**
	 * The maximum number of perks a player can have enabled simultaneously.
	 * If null, defaults to 5.
	 */
	private Integer maxEnabledPerksPerPlayer;
	
	/**
	 * Global multiplier for all perk cooldowns (1.0 = normal, 0.5 = half cooldown, 2.0 = double cooldown).
	 * If null, defaults to 1.0.
	 */
	private Double cooldownMultiplier;
	
	// ==================== UI Settings ====================
	
	/**
	 * Number of perks to display per page in the perk overview GUI.
	 * If null, defaults to 28.
	 */
	private Integer perksPerPage;
	
	/**
	 * Whether to show locked perks in the perk overview GUI.
	 * If null, defaults to true.
	 */
	private Boolean showLockedPerks;
	
	/**
	 * Whether to show requirement progress for locked perks.
	 * If null, defaults to true.
	 */
	private Boolean showRequirementProgress;
	
	// ==================== Notification Settings ====================
	
	/**
	 * Notification configuration for perk unlock events.
	 */
	private NotificationTypeSection unlockNotification;
	
	/**
	 * Notification configuration for perk activation events.
	 */
	private NotificationTypeSection activationNotification;
	
	/**
	 * Notification configuration for perk cooldown events.
	 */
	private NotificationTypeSection cooldownNotification;
	
	// ==================== Integration Settings ====================
	
	/**
	 * Whether to enable perk rewards in the rank system.
	 * If null, defaults to true.
	 */
	private Boolean enablePerkRewards;
	
	/**
	 * Whether to automatically activate perks when they are unlocked.
	 * If null, defaults to false.
	 */
	private Boolean enableAutoActivation;
	
	// ==================== Cache Settings ====================
	
	/**
	 * Whether perk caching is enabled.
	 * If null, defaults to true.
	 */
	private Boolean cacheEnabled;
	
	/**
	 * Maximum retry attempts for cache save operations.
	 * If null, defaults to 3.
	 */
	private Integer cacheMaxRetries;
	
	/**
	 * Base delay in milliseconds for retry backoff.
	 * If null, defaults to 100.
	 */
	private Long cacheRetryDelayMs;
	
	/**
	 * Timeout in seconds for cache save operations.
	 * If null, defaults to 10.
	 */
	private Integer cacheSaveTimeoutSeconds;
	
	/**
	 * Whether to log cache performance metrics.
	 * If null, defaults to true.
	 */
	private Boolean cacheLogPerformance;
	
	/**
	 * Performance threshold in milliseconds for warnings.
	 * If null, defaults to 500.
	 */
	private Long cachePerformanceThresholdMs;
	
	/**
	 * Constructs a new PerkSystemSection with the given evaluation environment.
	 *
	 * @param evaluationEnvironmentBuilder the evaluation environment builder
	 */
	public PerkSystemSection(
		final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder
	) {
		super(evaluationEnvironmentBuilder);
	}
	
	/**
	 * Called after parsing the configuration fields. Sets default values for missing fields.
	 *
	 * @param fields the list of fields parsed
	 * @throws Exception if an error occurs during post-processing
	 */
	@Override
	public void afterParsing(final List<Field> fields) throws Exception {
		super.afterParsing(fields);
		
		// Validate and set defaults
		if (maxEnabledPerksPerPlayer != null && maxEnabledPerksPerPlayer < 1) {
			LOGGER.warning("maxEnabledPerksPerPlayer must be at least 1, setting to default (5)");
			maxEnabledPerksPerPlayer = 5;
		}
		
		if (cooldownMultiplier != null && cooldownMultiplier < 0.0) {
			LOGGER.warning("cooldownMultiplier cannot be negative, setting to default (1.0)");
			cooldownMultiplier = 1.0;
		}
		
		if (perksPerPage != null && perksPerPage < 1) {
			LOGGER.warning("perksPerPage must be at least 1, setting to default (28)");
			perksPerPage = 28;
		}
		
		if (cacheMaxRetries != null && cacheMaxRetries < 1) {
			LOGGER.warning("cacheMaxRetries must be at least 1, setting to default (3)");
			cacheMaxRetries = 3;
		}
		
		if (cacheRetryDelayMs != null && cacheRetryDelayMs < 0) {
			LOGGER.warning("cacheRetryDelayMs cannot be negative, setting to default (100)");
			cacheRetryDelayMs = 100L;
		}
		
		if (cacheSaveTimeoutSeconds != null && cacheSaveTimeoutSeconds < 1) {
			LOGGER.warning("cacheSaveTimeoutSeconds must be at least 1, setting to default (10)");
			cacheSaveTimeoutSeconds = 10;
		}
		
		if (cachePerformanceThresholdMs != null && cachePerformanceThresholdMs < 0) {
			LOGGER.warning("cachePerformanceThresholdMs cannot be negative, setting to default (500)");
			cachePerformanceThresholdMs = 500L;
		}
		
		// Log configuration summary
		LOGGER.info("Perk system configuration loaded: enabled=" + getEnabled() + 
			", maxEnabledPerks=" + getMaxEnabledPerksPerPlayer() + 
			", cooldownMultiplier=" + getCooldownMultiplier());
	}
	
	// ==================== Getters ====================
	
	/**
	 * Gets enabled.
	 */
	public Boolean getEnabled() {
		return enabled == null || enabled;
	}
	
	/**
	 * Gets maxEnabledPerksPerPlayer.
	 */
	public Integer getMaxEnabledPerksPerPlayer() {
		return maxEnabledPerksPerPlayer == null ? 5 : maxEnabledPerksPerPlayer;
	}
	
	/**
	 * Gets cooldownMultiplier.
	 */
	public Double getCooldownMultiplier() {
		return cooldownMultiplier == null ? 1.0 : cooldownMultiplier;
	}
	
	/**
	 * Gets perksPerPage.
	 */
	public Integer getPerksPerPage() {
		return perksPerPage == null ? 28 : perksPerPage;
	}
	
	/**
	 * Gets showLockedPerks.
	 */
	public Boolean getShowLockedPerks() {
		return showLockedPerks == null || showLockedPerks;
	}
	
	/**
	 * Gets showRequirementProgress.
	 */
	public Boolean getShowRequirementProgress() {
		return showRequirementProgress == null || showRequirementProgress;
	}
	
	/**
	 * Gets unlockNotification.
	 */
	public NotificationTypeSection getUnlockNotification() {
		return unlockNotification == null ? 
			new NotificationTypeSection(new EvaluationEnvironmentBuilder()) : 
			unlockNotification;
	}
	
	/**
	 * Gets activationNotification.
	 */
	public NotificationTypeSection getActivationNotification() {
		return activationNotification == null ? 
			new NotificationTypeSection(new EvaluationEnvironmentBuilder()) : 
			activationNotification;
	}
	
	/**
	 * Gets cooldownNotification.
	 */
	public NotificationTypeSection getCooldownNotification() {
		return cooldownNotification == null ? 
			new NotificationTypeSection(new EvaluationEnvironmentBuilder()) : 
			cooldownNotification;
	}
	
	/**
	 * Gets enablePerkRewards.
	 */
	public Boolean getEnablePerkRewards() {
		return enablePerkRewards == null || enablePerkRewards;
	}
	
	/**
	 * Gets enableAutoActivation.
	 */
	public Boolean getEnableAutoActivation() {
		return enableAutoActivation != null && enableAutoActivation;
	}
	
	/**
	 * Gets cacheEnabled.
	 */
	public Boolean getCacheEnabled() {
		return cacheEnabled == null || cacheEnabled;
	}
	
	/**
	 * Gets cacheMaxRetries.
	 */
	public Integer getCacheMaxRetries() {
		return cacheMaxRetries == null ? 3 : cacheMaxRetries;
	}
	
	/**
	 * Gets cacheRetryDelayMs.
	 */
	public Long getCacheRetryDelayMs() {
		return cacheRetryDelayMs == null ? 100L : cacheRetryDelayMs;
	}
	
	/**
	 * Gets cacheSaveTimeoutSeconds.
	 */
	public Integer getCacheSaveTimeoutSeconds() {
		return cacheSaveTimeoutSeconds == null ? 10 : cacheSaveTimeoutSeconds;
	}
	
	/**
	 * Gets cacheLogPerformance.
	 */
	public Boolean getCacheLogPerformance() {
		return cacheLogPerformance == null || cacheLogPerformance;
	}
	
	/**
	 * Gets cachePerformanceThresholdMs.
	 */
	public Long getCachePerformanceThresholdMs() {
		return cachePerformanceThresholdMs == null ? 500L : cachePerformanceThresholdMs;
	}
}
