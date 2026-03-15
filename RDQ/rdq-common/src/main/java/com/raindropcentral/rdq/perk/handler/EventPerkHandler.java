package com.raindropcentral.rdq.perk.handler;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.config.perk.PerkEffectSection;
import com.raindropcentral.rdq.config.perk.PerkSection;
import com.raindropcentral.rdq.database.entity.perk.Perk;
import com.raindropcentral.rdq.database.entity.perk.PerkType;
import com.raindropcentral.rdq.database.entity.perk.PlayerPerk;
import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.evaluable.ConfigKeeper;
import de.jexcellence.evaluable.ConfigManager;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handler for event-triggered and percentage-based perks.
 *
 * <p>This handler manages the registration, processing, and triggering of perks that
 * activate in response to game events. It handles cooldown checking, trigger chance
 * calculation, and event processing.
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class EventPerkHandler {
	
	private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
	private static final String PERKS_PATH = "perks";
	
	private final RDQ plugin;
	private final Map<UUID, Map<String, Set<PlayerPerk>>> registeredPerks = new ConcurrentHashMap<>();
	private final Map<String, PerkSection> configCache = new ConcurrentHashMap<>();
	
	/**
	 * Executes EventPerkHandler.
	 */
	public EventPerkHandler(@NotNull final RDQ plugin) {
		this.plugin = plugin;
	}
	
	/**
	 * Registers an event-triggered perk for a player.
	 *
	 * @param player the Bukkit player
	 * @param playerPerk the player perk to register
	 * @return true if registered successfully, false otherwise
	 */
	public boolean registerEventPerk(@NotNull final Player player, @NotNull final PlayerPerk playerPerk) {
		LOGGER.info("Attempting to register event perk " + playerPerk.getPerk().getIdentifier() + " for player " + player.getName());
		
		PerkSection perkSection = loadPerkConfig(playerPerk.getPerk());
		if (perkSection == null || perkSection.getEffect() == null) {
			LOGGER.warning("Cannot register perk " + playerPerk.getPerk().getIdentifier() + " - no effect config");
			return false;
		}
		
		String triggerEvent = perkSection.getEffect().getTriggerEvent();
		if (triggerEvent == null || triggerEvent.isEmpty()) {
			LOGGER.warning("Event perk " + playerPerk.getPerk().getIdentifier() + " has no triggerEvent");
			return false;
		}
		
		registeredPerks
				.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
				.computeIfAbsent(triggerEvent.toUpperCase(), k -> ConcurrentHashMap.newKeySet())
				.add(playerPerk);
		
		LOGGER.info("Successfully registered event perk " + playerPerk.getPerk().getIdentifier() + 
				" for player " + player.getName() + " on event " + triggerEvent + 
				". Total registered perks for player: " + registeredPerks.get(player.getUniqueId()).size());
		return true;
	}
	
	/**
	 * Unregisters an event-triggered perk for a player.
	 *
	 * @param player the Bukkit player
	 * @param playerPerk the player perk to unregister
	 * @return true if unregistered successfully, false otherwise
	 */
	public boolean unregisterEventPerk(@NotNull final Player player, @NotNull final PlayerPerk playerPerk) {
		PerkSection perkSection = loadPerkConfig(playerPerk.getPerk());
		if (perkSection == null || perkSection.getEffect() == null) {
			return false;
		}
		
		String triggerEvent = perkSection.getEffect().getTriggerEvent();
		if (triggerEvent == null) {
			return false;
		}
		
		Map<String, Set<PlayerPerk>> playerEvents = registeredPerks.get(player.getUniqueId());
		if (playerEvents != null) {
			Set<PlayerPerk> perks = playerEvents.get(triggerEvent.toUpperCase());
			if (perks != null) {
				perks.remove(playerPerk);
				if (perks.isEmpty()) {
					playerEvents.remove(triggerEvent.toUpperCase());
				}
			}
			if (playerEvents.isEmpty()) {
				registeredPerks.remove(player.getUniqueId());
			}
		}
		
		return true;
	}
	
	/**
	 * Processes an event for a player, checking all registered perks for that event type.
	 *
	 * @param player the player
	 * @param eventType the type of event that occurred
	 * @param args additional event arguments
	 */
	public void processEvent(@NotNull final Player player, @NotNull final String eventType, @NotNull final Object... args) {
		LOGGER.fine("Processing event " + eventType + " for player " + player.getName());
		
		Map<String, Set<PlayerPerk>> playerEvents = registeredPerks.get(player.getUniqueId());
		if (playerEvents == null) {
			LOGGER.fine("No registered perks for player " + player.getName());
			return;
		}
		
		Set<PlayerPerk> perks = playerEvents.get(eventType.toUpperCase());
		if (perks == null || perks.isEmpty()) {
			LOGGER.fine("No perks registered for event " + eventType + " for player " + player.getName() + 
					". Available events: " + playerEvents.keySet());
			return;
		}
		
		LOGGER.fine("Processing event " + eventType + " for player " + player.getName() + " with " + perks.size() + " perks");
		
		for (PlayerPerk playerPerk : perks) {
			try {
				processEventPerk(player, playerPerk, args);
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Error processing perk " + playerPerk.getPerk().getIdentifier(), e);
			}
		}
	}
	
	/**
	 * Processes a single event perk for an event.
	 *
	 * @param player the player
	 * @param playerPerk the player perk
	 * @param args event arguments
	 */
	private void processEventPerk(@NotNull final Player player, @NotNull final PlayerPerk playerPerk, @NotNull final Object... args) {
		if (playerPerk.isOnCooldown()) {
			return;
		}
		
		PerkSection perkSection = loadPerkConfig(playerPerk.getPerk());
		if (perkSection == null || perkSection.getEffect() == null) {
			return;
		}
		
		if (!shouldTrigger(playerPerk, perkSection.getEffect())) {
			return;
		}
		
		LOGGER.info("Triggering perk " + playerPerk.getPerk().getIdentifier() + " for player " + player.getName());
		
		boolean effectApplied = applyEventEffect(player, perkSection.getEffect(), args);
		
		if (effectApplied) {
			plugin.getPerkActivationService().recordEffectTrigger(player, playerPerk);
			long cooldown = perkSection.getEffect().getCooldownMillis();
			if (cooldown > 0) {
				playerPerk.startCooldown(cooldown);
			}
		}
	}
	
	/**
	 * Determines if a perk should trigger based on its trigger chance.
	 *
	 * @param playerPerk the player perk
	 * @param effect the perk effect section
	 * @return true if the perk should trigger, false otherwise
	 */
	private boolean shouldTrigger(@NotNull final PlayerPerk playerPerk, @NotNull final PerkEffectSection effect) {
		if (playerPerk.getPerk().getPerkType() != PerkType.PERCENTAGE_BASED) {
			return true;
		}
		
		double chance = effect.getTriggerChance();
		return Math.random() * 100.0 <= chance;
	}
	
	/**
	 * Applies the effect of an event-triggered perk.
	 *
	 * @param player the player
	 * @param effect the perk effect section
	 * @param args event arguments
	 * @return true if the effect was successfully applied, false otherwise
	 */
	private boolean applyEventEffect(@NotNull final Player player, @NotNull final PerkEffectSection effect, @NotNull final Object... args) {
		Map<String, Object> customConfig = effect.getCustomConfig();
		if (customConfig == null || customConfig.isEmpty()) {
			return false;
		}
		
		boolean applied = false;
		
		if (customConfig.containsKey("rate") && args.length > 0 && args[0] instanceof PlayerItemConsumeEvent event) {
			applied = handlePotionExtend(player, event, ((Number) customConfig.get("rate")).doubleValue());
		}
		
		if (customConfig.containsKey("amplify") && args.length > 0 && args[0] instanceof PlayerItemConsumeEvent event) {
			applied = handlePotionAmplify(player, event, ((Number) customConfig.get("amplify")).doubleValue());
		}
		
		if (customConfig.containsKey("saved") && args.length > 0 && args[0] instanceof PlayerItemConsumeEvent event) {
			applied = handlePotionSave(player, event, ((Number) customConfig.get("saved")).intValue());
		}
		
		if (customConfig.containsKey("minWaitTime") && args.length > 0 && args[0] instanceof PlayerFishEvent event) {
			applied = handleFishingRate(player, event, customConfig);
		}
		
		if (customConfig.containsKey("vanilla") && args.length > 0 && args[0] instanceof PlayerFishEvent event) {
			applied = handleFishingXP(player, event, customConfig);
		}
		
		if (customConfig.containsKey("healAmount")) {
			applied = handleHeal(player, ((Number) customConfig.get("healAmount")).doubleValue());
		}
		
		if (customConfig.containsKey("playSound") && (Boolean) customConfig.get("playSound")) {
			playSound(player, (String) customConfig.get("soundType"));
		}
		
		return applied;
	}
	
	private boolean handlePotionExtend(@NotNull final Player player, @NotNull final PlayerItemConsumeEvent event, double rate) {
		if (!(event.getItem().getItemMeta() instanceof PotionMeta meta)) {
			return false;
		}
		
		// Cancel the event to prevent normal potion effects
		event.setCancelled(true);
		
		// Apply effects on next tick to ensure event cancellation is processed
		plugin.getPlatform().getScheduler().runSync(() -> {
			for (PotionEffect effect : meta.getAllEffects()) {
				player.addPotionEffect(new PotionEffect(
						effect.getType(),
						(int) (effect.getDuration() * rate),
						effect.getAmplifier(),
						effect.isAmbient(),
						effect.hasParticles(),
						effect.hasIcon()
				), true);
			}
			
			// Manually consume the item from the player's hand
			ItemStack itemInHand = player.getInventory().getItemInMainHand();
			if (itemInHand.getAmount() > 1) {
				itemInHand.setAmount(itemInHand.getAmount() - 1);
			} else {
				player.getInventory().setItemInMainHand(null);
			}
			
			new I18n.Builder("perk.messages.potion_extended", player).build().sendMessage();
		});
		
		return true;
	}
	
	private boolean handlePotionAmplify(@NotNull final Player player, @NotNull final PlayerItemConsumeEvent event, double amplify) {
		if (!(event.getItem().getItemMeta() instanceof PotionMeta meta)) {
			return false;
		}
		
		// Cancel the event to prevent normal potion effects
		event.setCancelled(true);
		
		// Apply effects on next tick to ensure event cancellation is processed
		plugin.getPlatform().getScheduler().runSync(() -> {
			for (PotionEffect effect : meta.getCustomEffects()) {
				player.addPotionEffect(new PotionEffect(
						effect.getType(),
						effect.getDuration(),
						(int) (effect.getAmplifier() * amplify),
						effect.isAmbient(),
						effect.hasParticles(),
						effect.hasIcon()
				), true);
			}
			
			// Manually consume the item from the player's hand
			ItemStack itemInHand = player.getInventory().getItemInMainHand();
			if (itemInHand.getAmount() > 1) {
				itemInHand.setAmount(itemInHand.getAmount() - 1);
			} else {
				player.getInventory().setItemInMainHand(null);
			}
			
			new I18n.Builder("perk.messages.potion_enhanced", player).build().sendMessage();
		});
		
		return true;
	}
	
	private boolean handlePotionSave(@NotNull final Player player, @NotNull final PlayerItemConsumeEvent event, int saved) {
		for (int i = 0; i < saved; i++) {
			player.getInventory().addItem(event.getItem().clone());
		}
		new I18n.Builder("perk.messages.potion_saved", player).build().sendMessage();
		return true;
	}
	
	private boolean handleFishingRate(@NotNull final Player player, @NotNull final PlayerFishEvent event, @NotNull final Map<String, Object> config) {
		if (event.getState() != PlayerFishEvent.State.FISHING) {
			return false;
		}
		
		FishHook hook = event.getHook();
		hook.setMinWaitTime(((Number) config.get("minWaitTime")).intValue());
		hook.setMaxWaitTime(((Number) config.get("maxWaitTime")).intValue());
		hook.setMinLureTime(((Number) config.get("minLureTime")).intValue());
		hook.setMaxLureTime(((Number) config.get("maxLureTime")).intValue());
		
		new I18n.Builder("perk.messages.fishing_active", player).build().sendMessage();
		return true;
	}
	
	private boolean handleFishingXP(@NotNull final Player player, @NotNull final PlayerFishEvent event, @NotNull final Map<String, Object> config) {
		if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
			return false;
		}
		
		int vanilla = ((Number) config.get("vanilla")).intValue();
		player.giveExp(vanilla);
		
		new I18n.Builder("perk.messages.fishing_xp", player)
				.withPlaceholder("xp", String.valueOf(vanilla))
				.build()
				.sendMessage();
		
		return true;
	}
	
	private boolean handleHeal(@NotNull final Player player, double amount) {
		double newHealth = Math.min(
				player.getHealth() + amount,
				player.getAttribute(Attribute.MAX_HEALTH).getValue()
		);
		player.setHealth(newHealth);
		return true;
	}
	
	private void playSound(@NotNull final Player player, @Nullable String soundType) {
		try {
			Sound sound = Sound.valueOf(soundType != null ? soundType : "ENTITY_PLAYER_LEVELUP");
			player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
		} catch (IllegalArgumentException e) {
			LOGGER.warning("Invalid sound type: " + soundType);
		}
	}
	
	@Nullable
	private PerkSection loadPerkConfig(@NotNull final Perk perk) {
		// Check cache first
		return configCache.computeIfAbsent(perk.getIdentifier(), identifier -> {
			try {
				ConfigManager cfgManager = new ConfigManager(plugin.getPlugin(), PERKS_PATH);
				ConfigKeeper<PerkSection> cfgKeeper = new ConfigKeeper<>(cfgManager, identifier + ".yml", PerkSection.class);
				return cfgKeeper.rootSection;
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Failed to load perk config for " + identifier, e);
				return null;
			}
		});
	}
	
	/**
	 * Clears the config cache. Call this when configs are reloaded.
	 */
	public void clearConfigCache() {
		configCache.clear();
		LOGGER.info("Cleared perk config cache");
	}
	
	/**
	 * Executes cleanupPlayer.
	 */
	public void cleanupPlayer(@NotNull final UUID playerUuid) {
		registeredPerks.remove(playerUuid);
	}
}
