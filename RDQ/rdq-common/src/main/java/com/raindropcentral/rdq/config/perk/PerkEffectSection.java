package com.raindropcentral.rdq.config.perk;

import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.configmapper.sections.CSIgnore;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Configuration section for perk effect definitions.
 *
 * <p>This section handles the effect configuration for perks, including potion effects,
 * event triggers, special abilities, and custom configurations.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
@CSAlways
public class PerkEffectSection extends AConfigSection {
	
	@CSIgnore
	private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
	
	// ==================== Potion Effect Fields ====================
	
	/**
	 * The potion effect type (e.g., SPEED, JUMP_BOOST, NIGHT_VISION).
	 * Used for passive potion effect perks.
	 */
	private String potionEffectType;
	
	/**
	 * The amplifier level for the potion effect (0 = level 1, 1 = level 2, etc.).
	 * If null, defaults to 0.
	 */
	private Integer amplifier;
	
	/**
	 * The duration of the potion effect in ticks (20 ticks = 1 second).
	 * For passive perks, this is the refresh interval.
	 * If null, defaults to 600 ticks (30 seconds).
	 */
	private Integer durationTicks;
	
	/**
	 * Whether the potion effect should be ambient (less visible particles).
	 * If null, defaults to false.
	 */
	private Boolean ambient;
	
	/**
	 * Whether the potion effect should show particles.
	 * If null, defaults to true.
	 */
	private Boolean particles;
	
	// ==================== Event-Triggered Fields ====================
	
	/**
	 * The event type that triggers this perk (e.g., ENTITY_DAMAGE_BY_ENTITY, PLAYER_DEATH).
	 * Used for event-triggered perks.
	 */
	private String triggerEvent;
	
	/**
	 * The cooldown duration in milliseconds before the perk can trigger again.
	 * If null, defaults to 0 (no cooldown).
	 */
	private Long cooldownMillis;
	
	/**
	 * The percentage chance (0.0-100.0) that the perk will trigger when the event occurs.
	 * Used for percentage-based perks.
	 * If null, defaults to 100.0 (always triggers).
	 */
	private Double triggerChance;
	
	// ==================== Special Ability Fields ====================
	
	/**
	 * The special ability type (e.g., FLY, GLOW, NO_FALL_DAMAGE, KEEP_INVENTORY, KEEP_EXPERIENCE).
	 * Used for special ability perks.
	 */
	private String specialType;
	
	// ==================== Custom Perk Fields ====================
	
	/**
	 * The fully qualified class name of a custom perk handler.
	 * Used for custom perk implementations.
	 */
	private String handlerClass;
	
	/**
	 * Custom configuration map for custom perk handlers.
	 * The structure depends on the specific handler implementation.
	 */
	private Map<String, Object> customConfig;
	
	/**
	 * Constructs a new PerkEffectSection with the given evaluation environment.
	 *
	 * @param evaluationEnvironmentBuilder the evaluation environment builder
	 */
	public PerkEffectSection(
		final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder
	) {
		super(evaluationEnvironmentBuilder);
	}
	
	/**
	 * Called after parsing the configuration fields. Validates the effect configuration.
	 *
	 * @param fields the list of fields parsed
	 * @throws Exception if an error occurs during post-processing
	 */
	@Override
	public void afterParsing(final List<Field> fields) throws Exception {
		super.afterParsing(fields);
		
		// Validate that at least one effect type is configured
		boolean hasEffect = false;
		
		if (potionEffectType != null && !potionEffectType.isEmpty()) {
			hasEffect = true;
			validatePotionEffect();
		}
		
		if (triggerEvent != null && !triggerEvent.isEmpty()) {
			hasEffect = true;
			validateEventTrigger();
		}
		
		if (specialType != null && !specialType.isEmpty()) {
			hasEffect = true;
			validateSpecialType();
		}
		
		if (handlerClass != null && !handlerClass.isEmpty()) {
			hasEffect = true;
		}
		
		if (!hasEffect) {
			LOGGER.warning("PerkEffectSection has no effect configured (no potionEffectType, triggerEvent, specialType, or handlerClass)");
		}
	}
	
	/**
	 * Validates potion effect configuration.
	 */
	private void validatePotionEffect() {
		if (amplifier != null && amplifier < 0) {
			LOGGER.warning("Potion effect amplifier cannot be negative, setting to 0");
			amplifier = 0;
		}
		
		if (durationTicks != null && durationTicks <= 0) {
			LOGGER.warning("Potion effect duration must be positive, setting to default (600 ticks)");
			durationTicks = 600;
		}
	}
	
	/**
	 * Validates event trigger configuration.
	 */
	private void validateEventTrigger() {
		if (cooldownMillis != null && cooldownMillis < 0) {
			LOGGER.warning("Cooldown cannot be negative, setting to 0");
			cooldownMillis = 0L;
		}
		
		if (triggerChance != null) {
			if (triggerChance < 0.0 || triggerChance > 100.0) {
				LOGGER.warning("Trigger chance must be between 0.0 and 100.0, clamping value");
				triggerChance = Math.max(0.0, Math.min(100.0, triggerChance));
			}
		}
	}
	
	/**
	 * Validates special type configuration.
	 */
	private void validateSpecialType() {
		String upperType = specialType.toUpperCase();
		switch (upperType) {
			case "FLY", "GLOW", "NO_FALL_DAMAGE", "KEEP_INVENTORY", "KEEP_EXPERIENCE" -> {
				// Valid special types
			}
			default -> LOGGER.warning("Unknown special type: " + specialType + 
				". Valid types: FLY, GLOW, NO_FALL_DAMAGE, KEEP_INVENTORY, KEEP_EXPERIENCE");
		}
	}
	
	// ==================== Getters ====================
	
	/**
	 * Gets potionEffectType.
	 */
	public String getPotionEffectType() {
		return potionEffectType;
	}
	
	/**
	 * Gets amplifier.
	 */
	public Integer getAmplifier() {
		return amplifier == null ? 0 : amplifier;
	}
	
	/**
	 * Gets durationTicks.
	 */
	public Integer getDurationTicks() {
		return durationTicks == null ? 600 : durationTicks;
	}
	
	/**
	 * Gets ambient.
	 */
	public Boolean getAmbient() {
		return ambient != null && ambient;
	}
	
	/**
	 * Gets particles.
	 */
	public Boolean getParticles() {
		return particles == null || particles;
	}
	
	/**
	 * Gets triggerEvent.
	 */
	public String getTriggerEvent() {
		return triggerEvent;
	}
	
	/**
	 * Gets cooldownMillis.
	 */
	public Long getCooldownMillis() {
		return cooldownMillis == null ? 0L : cooldownMillis;
	}
	
	/**
	 * Gets triggerChance.
	 */
	public Double getTriggerChance() {
		return triggerChance == null ? 100.0 : triggerChance;
	}
	
	/**
	 * Gets specialType.
	 */
	public String getSpecialType() {
		return specialType;
	}
	
	/**
	 * Gets handlerClass.
	 */
	public String getHandlerClass() {
		return handlerClass;
	}
	
	/**
	 * Gets customConfig.
	 */
	public Map<String, Object> getCustomConfig() {
		return customConfig;
	}
}
