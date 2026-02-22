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
 * <p>
 * This section handles system-wide perk configuration including enable/disable,
 * player limits, cooldown multipliers, UI settings, notifications, and integration settings.
 * Default values are provided for all fields if not set.
 * </p>
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
	
	public Boolean getEnabled() {
		return enabled == null || enabled;
	}
	
	public Integer getMaxEnabledPerksPerPlayer() {
		return maxEnabledPerksPerPlayer == null ? 5 : maxEnabledPerksPerPlayer;
	}
	
	public Double getCooldownMultiplier() {
		return cooldownMultiplier == null ? 1.0 : cooldownMultiplier;
	}
	
	public Integer getPerksPerPage() {
		return perksPerPage == null ? 28 : perksPerPage;
	}
	
	public Boolean getShowLockedPerks() {
		return showLockedPerks == null || showLockedPerks;
	}
	
	public Boolean getShowRequirementProgress() {
		return showRequirementProgress == null || showRequirementProgress;
	}
	
	public NotificationTypeSection getUnlockNotification() {
		return unlockNotification == null ? 
			new NotificationTypeSection(new EvaluationEnvironmentBuilder()) : 
			unlockNotification;
	}
	
	public NotificationTypeSection getActivationNotification() {
		return activationNotification == null ? 
			new NotificationTypeSection(new EvaluationEnvironmentBuilder()) : 
			activationNotification;
	}
	
	public NotificationTypeSection getCooldownNotification() {
		return cooldownNotification == null ? 
			new NotificationTypeSection(new EvaluationEnvironmentBuilder()) : 
			cooldownNotification;
	}
	
	public Boolean getEnablePerkRewards() {
		return enablePerkRewards == null || enablePerkRewards;
	}
	
	public Boolean getEnableAutoActivation() {
		return enableAutoActivation != null && enableAutoActivation;
	}
	
	public Boolean getCacheEnabled() {
		return cacheEnabled == null || cacheEnabled;
	}
	
	public Integer getCacheMaxRetries() {
		return cacheMaxRetries == null ? 3 : cacheMaxRetries;
	}
	
	public Long getCacheRetryDelayMs() {
		return cacheRetryDelayMs == null ? 100L : cacheRetryDelayMs;
	}
	
	public Integer getCacheSaveTimeoutSeconds() {
		return cacheSaveTimeoutSeconds == null ? 10 : cacheSaveTimeoutSeconds;
	}
	
	public Boolean getCacheLogPerformance() {
		return cacheLogPerformance == null || cacheLogPerformance;
	}
	
	public Long getCachePerformanceThresholdMs() {
		return cachePerformanceThresholdMs == null ? 500L : cachePerformanceThresholdMs;
	}
}
