package com.raindropcentral.rdq.view.perks;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.perk.Perk;
import com.raindropcentral.rdq.database.entity.perk.PerkRequirement;
import com.raindropcentral.rdq.database.entity.perk.PerkUnlockReward;
import com.raindropcentral.rdq.database.entity.perk.PlayerPerk;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.perk.PerkActivationService;
import com.raindropcentral.rdq.perk.PerkManagementService;
import com.raindropcentral.rdq.perk.PerkRequirementService;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.reward.AbstractReward;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Represents the PerkDetailView API type.
 */
public class PerkDetailView extends BaseView {
	
	private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
	
	private final State<RDQ> rdq = initialState("plugin");
	private final State<RDQPlayer> currentPlayer = initialState("player");
	private final State<Perk> targetPerk = initialState("perk");
	
	private static final int PERK_INFO_SLOT = 4;
	private static final int[] REQUIREMENT_SLOTS = {10, 11, 12, 13, 14, 15, 16};
	private static final int[] REWARD_SLOTS = {19, 20, 21, 22, 23, 24, 25};
	private static final int STATE_INFO_SLOT = 31;
	private static final int UNLOCK_BUTTON_SLOT = 38;
	private static final int TOGGLE_BUTTON_SLOT = 40;
	private static final int DISABLE_BUTTON_SLOT = 42;
	
	/**
	 * Executes PerkDetailView.
	 */
	public PerkDetailView() {
		super(PerkOverviewView.class);
	}
	
	@Override
	protected String getKey() {
		return "perk_detail_ui";
	}
	
	@Override
	protected @NotNull Map<String, Object> getTitlePlaceholders(final @NotNull OpenContext openContext) {
		final Perk perk = targetPerk.get(openContext);
		return Map.of("perk_name", perk != null ? perk.getIdentifier() : "Unknown");
	}
	
	/**
	 * Executes onFirstRender.
	 */
	@Override
	public void onFirstRender(final @NotNull RenderContext render, final @NotNull Player player) {
		final Perk perk = targetPerk.get(render);
		final RDQPlayer rdqPlayer = currentPlayer.get(render);
		final RDQ plugin = rdq.get(render);
		
		if (perk == null) {
			renderErrorState(render);
			return;
		}
		
		renderPerkHeader(render, player, perk);
		renderRequirements(render, player, perk, rdqPlayer, plugin);
		renderUnlockRewards(render, player, perk);
		renderPerkState(render, player, perk, rdqPlayer, plugin);
		renderActions(render, player, perk, rdqPlayer, plugin);
	}
	
	private void renderPerkHeader(@NotNull final RenderContext render, @NotNull final Player player, @NotNull final Perk perk) {
		render.slot(PERK_INFO_SLOT).renderWith(() -> {
			try {
				final Material icon = Material.valueOf(perk.getIcon().getMaterial().toUpperCase());
				final Component name = new I18n.Builder(perk.getIcon().getDisplayNameKey(), player).build().component();
				final Component description = new I18n.Builder(perk.getIcon().getDescriptionKey(), player).build().component();
				
				final List<Component> lore = new ArrayList<>();
				lore.add(Component.empty());
				lore.add(description);
				lore.add(Component.empty());
				lore.add(new I18n.Builder("perk.ui.header.type_label", player)
						.withPlaceholder("type", formatPerkType(perk.getPerkType()))
						.build()
						.component());
				lore.add(new I18n.Builder("perk.ui.header.category_label", player)
						.withPlaceholder("category", formatPerkCategory(perk.getCategory()))
						.build()
						.component());
				
				return UnifiedBuilderFactory.item(icon)
						.setName(name)
						.setLore(lore)
						.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS)
						.build();
			} catch (final Exception e) {
				return UnifiedBuilderFactory.item(Material.BOOK)
						.setName(Component.text(perk.getIdentifier()))
						.build();
			}
		});
	}
	
	private void renderRequirements(
			@NotNull final RenderContext render,
			@NotNull final Player player,
			@NotNull final Perk perk,
			@NotNull final RDQPlayer rdqPlayer,
			@NotNull final RDQ plugin
	) {
		final List<PerkRequirement> requirements = perk.getRequirements().stream()
				.sorted(Comparator.comparingInt(PerkRequirement::getDisplayOrder))
				.collect(Collectors.toList());
		
		if (requirements.isEmpty()) {
			render.slot(REQUIREMENT_SLOTS[3]).renderWith(() -> createNoRequirementsCard(player));
			return;
		}
		
		final PerkRequirementService requirementService = plugin.getPerkRequirementService();
		final com.raindropcentral.rdq.view.perks.util.PerkRequirementCardRenderer cardRenderer =
				new com.raindropcentral.rdq.view.perks.util.PerkRequirementCardRenderer(requirementService);
		
		for (int i = 0; i < Math.min(REQUIREMENT_SLOTS.length, requirements.size()); i++) {
			final PerkRequirement requirement = requirements.get(i);
			final int slot = REQUIREMENT_SLOTS[i];
			render.slot(slot).renderWith(() -> cardRenderer.createEnhancedRequirementCard(player, requirement));
		}
	}
	
	private @NotNull ItemStack createNoRequirementsCard(@NotNull final Player player) {
		return UnifiedBuilderFactory.item(Material.LIME_STAINED_GLASS_PANE)
				.setName(new I18n.Builder("perk.ui.requirements.no_requirements", player).build().component())
				.setLore(List.of(new I18n.Builder("perk.ui.requirements.no_requirements_lore", player).build().component()))
				.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
				.build();
	}
	
	private void renderUnlockRewards(@NotNull final RenderContext render, @NotNull final Player player, @NotNull final Perk perk) {
		final List<PerkUnlockReward> rewards = perk.getUnlockRewards().stream()
				.sorted(Comparator.comparingInt(PerkUnlockReward::getDisplayOrder))
				.collect(Collectors.toList());
		
		if (rewards.isEmpty()) {
			render.slot(REWARD_SLOTS[3]).renderWith(() -> createNoRewardsCard(player));
			return;
		}
		
		for (int i = 0; i < Math.min(REWARD_SLOTS.length, rewards.size()); i++) {
			final PerkUnlockReward reward = rewards.get(i);
			final int slot = REWARD_SLOTS[i];
			render.slot(slot).renderWith(() -> createRewardCard(player, reward));
		}
	}
	
	private @NotNull ItemStack createRewardCard(@NotNull final Player player, @NotNull final PerkUnlockReward reward) {
		try {
			final AbstractReward abstractReward = reward.getReward();
			final Material icon = reward.getIcon() != null ?
					Material.valueOf(reward.getIcon().getMaterial().toUpperCase()) : Material.DIAMOND;
			
			final List<Component> lore = new ArrayList<>();
			lore.add(Component.empty());
			lore.add(new I18n.Builder("perk.ui.rewards.type_label", player)
					.withPlaceholder("type", abstractReward.getTypeId())
					.build()
					.component());
			
			final double value = abstractReward.getEstimatedValue();
			if (value > 0) {
				lore.add(new I18n.Builder("perk.ui.rewards.value_label", player)
						.withPlaceholder("value", String.format("%.2f", value))
						.build()
						.component());
			}
			
			return UnifiedBuilderFactory.item(icon)
					.setName(new I18n.Builder("perk.ui.rewards.title", player).build().component())
					.setLore(lore)
					.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS)
					.build();
		} catch (final Exception e) {
			return UnifiedBuilderFactory.item(Material.DIAMOND)
					.setName(Component.text("Reward"))
					.build();
		}
	}
	
	private @NotNull ItemStack createNoRewardsCard(@NotNull final Player player) {
		return UnifiedBuilderFactory.item(Material.GRAY_STAINED_GLASS_PANE)
				.setName(new I18n.Builder("perk.ui.rewards.no_rewards", player).build().component())
				.setLore(List.of(new I18n.Builder("perk.ui.rewards.no_rewards_lore", player).build().component()))
				.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
				.build();
	}
	
	private void renderPerkState(
			@NotNull final RenderContext render,
			@NotNull final Player player,
			@NotNull final Perk perk,
			@NotNull final RDQPlayer rdqPlayer,
			@NotNull final RDQ plugin
	) {
		render.slot(STATE_INFO_SLOT).renderWith(() -> {
			final PerkManagementService managementService = plugin.getPerkManagementService();
			final Optional<PlayerPerk> playerPerkOpt = managementService.getPlayerPerk(rdqPlayer, perk);
			return createStateInfoCard(player, playerPerkOpt.orElse(null), managementService, rdqPlayer);
		});
	}
	
	private @NotNull ItemStack createStateInfoCard(
			@NotNull final Player player,
			@org.jetbrains.annotations.Nullable final PlayerPerk playerPerk,
			@NotNull final PerkManagementService managementService,
			@NotNull final RDQPlayer rdqPlayer
	) {
		final List<Component> lore = new ArrayList<>();
		lore.add(Component.empty());
		
		if (playerPerk == null || !playerPerk.isUnlocked()) {
			lore.add(new I18n.Builder("perk.ui.status.locked", player).build().component());
			lore.add(Component.empty());
			lore.add(new I18n.Builder("perk.ui.status.locked_hint", player).build().component());
			
			return UnifiedBuilderFactory.item(Material.RED_STAINED_GLASS_PANE)
					.setName(new I18n.Builder("perk.ui.status.title", player).build().component())
					.setLore(lore)
					.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
					.build();
		}
		
		if (playerPerk.isActive()) {
			lore.add(new I18n.Builder("perk.ui.status.active", player).build().component());
		} else if (playerPerk.isEnabled()) {
			lore.add(new I18n.Builder("perk.ui.status.enabled", player).build().component());
		} else {
			lore.add(new I18n.Builder("perk.ui.status.disabled", player).build().component());
		}
		
		lore.add(Component.empty());
		
		final int enabledCount = managementService.getEnabledPerkCount(rdqPlayer);
		final int maxEnabled = managementService.getMaxEnabledPerks();
		lore.add(new I18n.Builder("perk.ui.status.enabled_count", player)
				.withPlaceholder("count", String.valueOf(enabledCount))
				.withPlaceholder("max", String.valueOf(maxEnabled))
				.build()
				.component());
		
		if (playerPerk.isOnCooldown()) {
			final long remainingMillis = playerPerk.getRemainingCooldownMillis();
			final String timeString = formatDuration(remainingMillis);
			lore.add(new I18n.Builder("perk.ui.status.cooldown_label", player)
					.withPlaceholder("time", timeString)
					.build()
					.component());
		}
		
		lore.add(Component.empty());
		lore.add(new I18n.Builder("perk.ui.status.activations_label", player)
				.withPlaceholder("count", String.valueOf(playerPerk.getActivationCount()))
				.build()
				.component());
		
		final Material material = playerPerk.isActive() ? Material.LIME_STAINED_GLASS_PANE :
				playerPerk.isEnabled() ? Material.YELLOW_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
		
		return UnifiedBuilderFactory.item(material)
				.setName(new I18n.Builder("perk.ui.status.title", player).build().component())
				.setLore(lore)
				.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
				.build();
	}
	
	private void renderActions(
			@NotNull final RenderContext render,
			@NotNull final Player player,
			@NotNull final Perk perk,
			@NotNull final RDQPlayer rdqPlayer,
			@NotNull final RDQ plugin
	) {
		final PerkManagementService managementService = plugin.getPerkManagementService();
		final Optional<PlayerPerk> playerPerkOpt = managementService.getPlayerPerk(rdqPlayer, perk);
		
		if (playerPerkOpt.isEmpty() || !playerPerkOpt.get().isUnlocked()) {
			renderUnlockButton(render, player, perk, rdqPlayer, plugin);
		} else {
			final PlayerPerk playerPerk = playerPerkOpt.get();
			if (playerPerk.isEnabled()) {
				renderDisableButton(render, player, perk);
			} else {
				renderEnableButton(render, player, perk, rdqPlayer, plugin);
			}
		}
	}
	
	private void renderUnlockButton(
			@NotNull final RenderContext render,
			@NotNull final Player player,
			@NotNull final Perk perk,
			@NotNull final RDQPlayer rdqPlayer,
			@NotNull final RDQ plugin
	) {
		render.slot(UNLOCK_BUTTON_SLOT)
				.renderWith(() -> {
					final List<Component> lore = new ArrayList<>();
					lore.add(Component.empty());
					lore.add(new I18n.Builder("perk.ui.buttons.unlock.hint", player).build().component());
					
					return UnifiedBuilderFactory.item(Material.LIME_DYE)
							.setName(new I18n.Builder("perk.ui.buttons.unlock.title", player).build().component())
							.setLore(lore)
							.setGlowing(true)
							.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
							.build();
				})
				.onClick(clickContext -> handleUnlockAttempt(clickContext, perk));
	}
	
	private void renderEnableButton(
			@NotNull final RenderContext render,
			@NotNull final Player player,
			@NotNull final Perk perk,
			@NotNull final RDQPlayer rdqPlayer,
			@NotNull final RDQ plugin
	) {
		render.slot(TOGGLE_BUTTON_SLOT)
				.renderWith(() -> {
					final PerkManagementService managementService = plugin.getPerkManagementService();
					final boolean canEnable = managementService.canEnableAnotherPerk(rdqPlayer);
					
					final List<Component> lore = new ArrayList<>();
					lore.add(Component.empty());
					
					if (canEnable) {
						lore.add(new I18n.Builder("perk.ui.buttons.enable.hint", player).build().component());
					} else {
						lore.add(new I18n.Builder("perk.ui.buttons.enable.limit_reached", player).build().component());
					}
					
					return UnifiedBuilderFactory.item(canEnable ? Material.LIME_DYE : Material.RED_DYE)
							.setName(new I18n.Builder("perk.ui.buttons.enable.title", player).build().component())
							.setLore(lore)
							.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
							.build();
				})
				.onClick(clickContext -> handleToggleEnable(clickContext, perk));
	}
	
	private void renderDisableButton(@NotNull final RenderContext render, @NotNull final Player player, @NotNull final Perk perk) {
		render.slot(DISABLE_BUTTON_SLOT)
				.renderWith(() -> {
					final List<Component> lore = new ArrayList<>();
					lore.add(Component.empty());
					lore.add(new I18n.Builder("perk.ui.buttons.disable.hint", player).build().component());
					
					return UnifiedBuilderFactory.item(Material.ORANGE_DYE)
							.setName(new I18n.Builder("perk.ui.buttons.disable.title", player).build().component())
							.setLore(lore)
							.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
							.build();
				})
				.onClick(clickContext -> handleToggleEnable(clickContext, perk));
	}
	
	private void handleUnlockAttempt(@NotNull final SlotClickContext clickContext, @NotNull final Perk perk) {
		final Player player = clickContext.getPlayer();
		final RDQ plugin = rdq.get(clickContext);
		final RDQPlayer rdqPlayer = currentPlayer.get(clickContext);
		final PerkRequirementService requirementService = plugin.getPerkRequirementService();
		
		if (!requirementService.canUnlock(player, perk)) {
			player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
			new I18n.Builder("perk.messages.unlock.requirements_not_met", player).build().sendMessage();
			return;
		}
		
		requirementService.attemptUnlock(player, rdqPlayer, perk).thenAccept(result -> {
			if (result.isSuccess()) {
				PlayerPerk playerPerk = result.getPlayerPerk();
				
				if (playerPerk != null && playerPerk.isUnlocked() && playerPerk.isEnabled()) {
					final PerkActivationService activationService = plugin.getPerkActivationService();
					activationService.activate(player, playerPerk).thenAccept(activated -> {
						player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
						new I18n.Builder("perk.messages.unlock.success", player)
								.withPlaceholder("perk", perk.getIdentifier())
								.build()
								.sendMessage();
						clickContext.closeForPlayer();
					});
				} else {
					LOGGER.log(Level.WARNING, "Perk {0} unlocked but not properly enabled", perk.getIdentifier());
					player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
					new I18n.Builder("perk.messages.unlock.failed", player).build().sendMessage();
					clickContext.update();
				}
			} else {
				player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
				new I18n.Builder("perk.messages.unlock.failed", player).build().sendMessage();
			}
		}).exceptionally(throwable -> {
			LOGGER.log(Level.SEVERE, "Failed to unlock perk " + perk.getIdentifier(), throwable);
			player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
			new I18n.Builder("perk.messages.unlock.error", player).build().sendMessage();
			return null;
		});
	}
	
	private void handleToggleEnable(@NotNull final SlotClickContext clickContext, @NotNull final Perk perk) {
		final Player player = clickContext.getPlayer();
		final RDQ plugin = rdq.get(clickContext);
		final RDQPlayer rdqPlayer = currentPlayer.get(clickContext);
		final PerkManagementService managementService = plugin.getPerkManagementService();
		final PerkActivationService activationService = plugin.getPerkActivationService();
		
		final Optional<PlayerPerk> playerPerkOpt = managementService.getPlayerPerk(rdqPlayer, perk);
		
		if (playerPerkOpt.isEmpty() || !playerPerkOpt.get().isUnlocked()) {
			player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
			new I18n.Builder("perk.messages.unlock.requirements_not_met", player).build().sendMessage();
			return;
		}
		
		final PlayerPerk playerPerk = playerPerkOpt.get();
		
		if (playerPerk.isEnabled()) {
			boolean success = managementService.disablePerk(rdqPlayer, perk);
			if (success) {
				if (playerPerk.isActive()) {
					activationService.deactivate(player, playerPerk);
				}
				
				player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 1.0f, 0.8f);
				new I18n.Builder("perk.messages.disable.success", player)
						.withPlaceholder("perk", perk.getIdentifier())
						.build()
						.sendMessage();
				clickContext.update();
			} else {
				player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
				new I18n.Builder("perk.messages.disable.failed", player)
						.withPlaceholder("perk", perk.getIdentifier())
						.build()
						.sendMessage();
			}
		} else {
			boolean success = managementService.enablePerk(rdqPlayer, perk);
			if (success) {
				activationService.activate(player, playerPerk);
				
				player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 1.0f, 1.2f);
				new I18n.Builder("perk.messages.enable.success", player)
						.withPlaceholder("perk", perk.getIdentifier())
						.build()
						.sendMessage();
				clickContext.update();
			} else {
				player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
				
				if (!managementService.canEnableAnotherPerk(rdqPlayer)) {
					new I18n.Builder("perk.messages.enable.limit_reached", player)
							.withPlaceholder("limit", String.valueOf(managementService.getMaxEnabledPerks()))
							.build()
							.sendMessage();
				} else {
					new I18n.Builder("perk.messages.enable.failed", player)
							.withPlaceholder("perk", perk.getIdentifier())
							.build()
							.sendMessage();
				}
			}
		}
	}
	
	private @NotNull String formatPerkType(@NotNull final com.raindropcentral.rdq.database.entity.perk.PerkType perkType) {
		return switch (perkType) {
			case PASSIVE -> "⚡ Passive";
			case EVENT_TRIGGERED -> "🎯 Event Triggered";
			case COOLDOWN_BASED -> "⏱ Cooldown Based";
			case PERCENTAGE_BASED -> "🎲 Percentage Based";
		};
	}
	
	private @NotNull String formatPerkCategory(@NotNull final com.raindropcentral.rdq.database.entity.perk.PerkCategory category) {
		return switch (category) {
			case COMBAT -> "Combat";
			case MOVEMENT -> "Movement";
			case UTILITY -> "Utility";
			case SURVIVAL -> " Survival";
			case ECONOMY -> "Economy";
			case SOCIAL -> "Social";
			case COSMETIC -> "Cosmetic";
			case SPECIAL -> "Special";
		};
	}
	
	private @NotNull String formatDuration(final long millis) {
		final long hours = millis / 3600000;
		final long minutes = (millis % 3600000) / 60000;
		final long seconds = (millis % 60000) / 1000;
		
		if (hours > 0) {
			return String.format("%dh %dm %ds", hours, minutes, seconds);
		} else if (minutes > 0) {
			return String.format("%dm %ds", minutes, seconds);
		} else {
			return String.format("%ds", seconds);
		}
	}
	
	private void renderErrorState(@NotNull final RenderContext render) {
		render.slot(22).renderWith(() -> UnifiedBuilderFactory.item(Material.BARRIER)
				.setName(Component.text("Error loading perk details"))
				.build());
	}
}
