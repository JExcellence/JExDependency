package com.raindropcentral.rdq.view.bounty;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.bounty.Bounty;
import com.raindropcentral.rdq.database.entity.bounty.BountyReward;
import com.raindropcentral.rplatform.reward.impl.ItemReward;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import com.raindropcentral.rplatform.view.PaginatedPlayerView;
import me.devnatan.inventoryframework.context.CloseContext;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.MutableState;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * InventoryFramework view for creating a new bounty in the RaindropQuests system.
 *
 * <p>This view allows players to:
 * <ul>
 *   <li>Select a target player for the bounty</li>
 *   <li>Add item and currency rewards</li>
 *   <li>Confirm and submit the bounty</li>
 * </ul>
 * The view manages state for the selected target, rewards, and inserted items,
 * and provides feedback and validation throughout the bounty creation process.
 *
 *
 * <p>Navigation to related views (such as {@link PaginatedPlayerView}, {@link BountyRewardView}, and {@link BountyPlayerInfoView})
 * is handled based on user actions and current state.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class BountyCreationView extends BaseView {

	private static final Logger LOGGER = Logger.getLogger("RDQ");

	private final State<RDQ> rdq = initialState("plugin");
	private final MutableState<Optional<OfflinePlayer>> target = initialState("target");
	private final MutableState<List<BountyReward>> rewards = initialState("rewards");
	private final State<Optional<Bounty>> bounty = initialState("bounty");
	private final State<Map<UUID, Map<Integer, ItemStack>>> insertedItems = initialState("insertedItems");

	private boolean isReopening;
	
	/**
	 * Computed state for the "Select Target" button, displaying the selected player or a prompt.
	 */
	private final State<ItemStack> targetSelectorButton = computedState(context -> {
		Player                  player       = context.getPlayer();
		Optional<OfflinePlayer> targetPlayer = this.target.get(context);
		String                  targetName   = targetPlayer.map(OfflinePlayer::getName).orElse("");
		return UnifiedBuilderFactory
			       .unifiedHead(targetPlayer.orElse(null))
			       .setDisplayName(
				       (net.kyori.adventure.text.Component) this.i18n(
					       "select_target.name",
					       player
				       ).withPlaceholder(
					       "target_name",
					       targetName
				       ).build().component()
			       )
			       .setLore(
				       this.i18n(
					       "select_target.lore",
					       player
				       ).withPlaceholder(
					       "target_name",
					       targetName
				       ).build().children()
			       )
			       .build();
	});
	
	/**
	 * Computed state for the "Add Items" button, enabled only if a target is selected.
	 */
	private final State<ItemStack> itemAdderButton = computedState(context -> {
		Player  player  = context.getPlayer();
		boolean enabled = this.target.get(context).isPresent();
		List<BountyReward> bountyRewards = new ArrayList<>(this.rewards.get(context));
		bountyRewards = bountyRewards.stream().filter(bountyReward -> bountyReward.getReward() instanceof ItemReward).toList();
		return UnifiedBuilderFactory
			       .item(enabled ?
			             Material.CHEST :
			             Material.BARRIER)
			       .setName(
				       this.i18n(
					       "add_items." + (
						       enabled ?
						       "name" :
						       "name_disabled"
					       ),
					       player
				       ).build().component()
			       )
			       .setLore(
				       this.i18n(
					       "add_items." + (
						       enabled ?
						       "description" :
						       "description_disabled"
					       ),
					       player
				       ).withPlaceholder(
					       "count",
					       bountyRewards.size()
				       ).build().children()
			       )
			       .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
			       .build();
	});
	
	/**
	 * Computed state for the "Add Currency" button, enabled only if a target is selected.
	 */
	private final State<ItemStack> currencyAdderButton = computedState(context -> {
		Player  player  = context.getPlayer();
		boolean enabled = this.target.get(context).isPresent();
		List<BountyReward> bountyRewards = new ArrayList<>(this.rewards.get(context));
		bountyRewards = bountyRewards.stream().filter(bountyReward -> bountyReward.getReward() instanceof com.raindropcentral.rplatform.reward.impl.CurrencyReward).toList();
		return UnifiedBuilderFactory
			       .item(enabled ?
			             Material.GOLD_INGOT :
			             Material.BARRIER)
			       .setName(
				       this.i18n(
					       "add_currency." + (
						       enabled ?
						       "name" :
						       "name_disabled"
					       ),
					       player
				       ).build().component()
			       )
			       .setLore(
				       this.i18n(
					       "add_currency." + (
						       enabled ?
						       "description" :
						       "description_disabled"
					       ),
					       player
				       ).withPlaceholder(
					       "count",
							   bountyRewards.size()
				       ).build().children()
			       )
			       .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
			       .build();
	});
	
	/**
	 * Computed state for the "Confirm" button, enabled only if a target and at least one reward item are selected.
	 */
	private final State<ItemStack> confirmButton = computedState(context -> {
		Player  player     = context.getPlayer();
		boolean canConfirm = this.target.get(context).isPresent() && ! this.rewards.get(context).isEmpty();
		return UnifiedBuilderFactory
			       .item(canConfirm ?
			             Material.EMERALD_BLOCK :
			             Material.REDSTONE_BLOCK)
			       .setName(
				       this.i18n(
					       "confirm_bounty.name",
					       player
				       ).build().component()
			       )
			       .setLore(
				       this.i18n(
					       "confirm_bounty.lore",
					       player
				       ).build().children()
			       )
			       .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
			       .build();
	});
	
	@Override
	protected int getSize() {
		
		return 5;
	}
	
	@Override
	protected Map<String, Object> getTitlePlaceholders(@NotNull final OpenContext open) {


		return Map.of(
			"target_name",
			this.target.get(open).map(OfflinePlayer::getName).orElse("not_defined")
		);
	}
	
	@Override
	protected String getKey() {
		
		return "bounty_creation_ui";
	}
	
	/**
	 * Executes onFirstRender.
	 */
	@Override
	public void onFirstRender(
		final @NotNull RenderContext render,
		final @NotNull Player player
	) {
		
		render
			.slot(11)
			.watch(this.targetSelectorButton)
			.renderWith(() -> this.targetSelectorButton.get(render))
			.onClick(context -> {
				this.isReopening = true;
				context.openForPlayer(
					PaginatedPlayerView.class,
					Map.of(
						"titleKey",
						"bounty_creation_ui.select_target.title",
						"parentClazz",
						this.getClass(),
						"initialData",
						context.getInitialData()
					)
				);
			})
		;
		
		render
			.slot(13, this.itemAdderButton.get(render))
			.watch(this.itemAdderButton)
			.renderWith(() -> this.itemAdderButton.get(render))
			.updateOnStateChange(this.itemAdderButton)
			.displayIf(() -> this.target.get(render).isPresent())
			.onClick(context -> {
				if (
					this.target.get(context).isEmpty()
				) {
					this.i18n(
						"add_items.disabled",
						player
					).includePrefix().build().sendMessage();
					return;
				}
				this.isReopening = true;
				
				if (
					this.bounty.get(context).isPresent()
				) {
					context.openForPlayer(
						BountyPlayerInfoView.class,
							Map.of(
									"plugin",
									rdq.get(context),
									"bounty",
									bounty.get(context),
									"target",
									target.get(context),
									"rewards",
									rewards.get(context),
									"insertedItems",
									insertedItems.get(context)
							)
					);
					return;
				}
				
				var foundBounty = this.rdq.get(context).getBountyRepository().findByAttributes(
					Map.of(
						"targetUniqueId",
						this.target.get(context).get().getUniqueId()
					)
				);
				
				if (foundBounty.isPresent()) {
					context.openForPlayer(
						BountyPlayerInfoView.class,
							Map.of(
									"plugin",
									rdq.get(context),
									"bounty",
									foundBounty,
									"target",
									target.get(context),
									"rewards",
									rewards.get(context),
									"insertedItems",
									insertedItems.get(context)
							)
					);
				} else {
					context.openForPlayer(
						BountyRewardView.class,
							Map.of(
									"plugin",
									rdq.get(context),
									"bounty",
									bounty.get(context),
									"target",
									target.get(context),
									"rewards",
									rewards.get(context),
									"insertedItems",
									insertedItems.get(context)
							)
					);
				}
			})
		;
		
		render
			.slot(15, this.currencyAdderButton.get(render))
			.watch(this.currencyAdderButton)
			.renderWith(() -> this.currencyAdderButton.get(render))
			.displayIf(() -> this.target.get(render).isPresent())
		;
		
		render
			.slot(31)
			.watch(this.confirmButton)
			.renderWith(() -> this.confirmButton.get(render))
			.displayIf(() -> this.target.get(render).isPresent() && ! this.rewards.get(render).isEmpty())
			.onClick(clickContext -> {
				if (
					this.target.get(clickContext).isEmpty()
				) {
					this.i18n(
						"confirm.no_player_selected",
						player
					).includePrefix().build().sendMessage();
					return;
				}
				
				if (
					this.rewards.get(clickContext).isEmpty()
				) {
					this.i18n(
						"confirm.no_rewards_selected",
						player
					).includePrefix().build().sendMessage();
					return;
				}
				
				this.i18n(
					"confirm.confirmation",
					player
				).includePrefix().withPlaceholder(
					"target_name",
					this.target.get(clickContext).map(OfflinePlayer::getName).orElse("not_defined")
				).build().sendMessage();
				
				final RDQ                     rdq    = this.rdq.get(clickContext);
				final Optional<OfflinePlayer> target = this.target.get(clickContext);
				
				if (
					target.isEmpty()
				) {
					this.i18n(
						"confirm.error",
						player
					).includePrefix().withPlaceholder(
						"target_name",
						"not_defined"
					).build().sendMessage();
					return;
				}
				
				CompletableFuture.supplyAsync(
					() -> rdq.getBountyRepository().findByAttributes(
						Map.of("targetUniqueId", target.get().getUniqueId())
					).orElse(null),
					rdq.getExecutor()
				).thenAcceptAsync(
						bounty -> {
							if (bounty != null) {
								// Update existing bounty with new reward
								rdq.getPlatform().getScheduler().runSync(() -> {
									LOGGER.info("=== BOUNTY UPDATE START ===");
									LOGGER.info("Found existing bounty with " + bounty.getRewards().size() + " rewards");
									LOGGER.info("UI rewards list has " + this.rewards.get(clickContext).size() + " rewards");

									// Debug: Log what's in the UI rewards list BEFORE merging
									for (int i = 0; i < this.rewards.get(clickContext).size(); i++) {
										BountyReward reward = this.rewards.get(clickContext).get(i);
										if (reward.getReward() instanceof ItemReward) {
											ItemReward itemReward = (ItemReward) reward.getReward();
											LOGGER.info("UI reward [" + i + "] BEFORE merge: " + itemReward.getItem().getType() +
													" x" + itemReward.getItem().getAmount());
										}
									}

									LOGGER.info("About to call mergeSimilarRewardItems with " + this.rewards.get(clickContext).size() + " rewards");
									List<BountyReward> newRewards = this.mergeSimilarRewardItems(this.rewards.get(clickContext));
									LOGGER.info("mergeSimilarRewardItems returned " + newRewards.size() + " rewards");
									LOGGER.info("Updating existing bounty for " + target.get().getName() + " with " + newRewards.size() + " new rewards");
									LOGGER.info("Current bounty has " + bounty.getRewards().size() + " existing rewards");

									// Debug: Log what we're sending to addRewardsToBounty
									for (int i = 0; i < newRewards.size(); i++) {
										BountyReward reward = newRewards.get(i);
										if (reward.getReward() instanceof ItemReward) {
											ItemReward itemReward = (ItemReward) reward.getReward();
											LOGGER.info("Sending to BountyFactory [" + i + "]: " + itemReward.getItem().getType() +
													" x" + itemReward.getItem().getAmount());
										}
									}

									// Send immediate confirmation that we're processing the update
									this.i18n(
											"bounty_creation.processing",
											player
									).includePrefix().build().sendMessage();

									// Use BountyFactory's addRewardsToBounty method to avoid entity conflicts
									LOGGER.info("Calling addRewardsToBounty on BountyFactory...");
									rdq.getBountyFactory().addRewardsToBounty(bounty.getTargetUniqueId(), newRewards)
											.thenAccept(updatedBounty -> {
												LOGGER.info("AddRewardsToBounty completed successfully for " + target.get().getName());

												// Always run on main thread
												rdq.getPlatform().getScheduler().runSync(() -> {
													LOGGER.info("Running success callback on main thread for " + player.getName());

													// Apply visual indicators to the target player if they're online
													Player targetPlayer = Bukkit.getPlayer(target.get().getUniqueId());
													if (targetPlayer != null && targetPlayer.isOnline()) {
														// Force refresh visual indicators for updated bounty
														rdq.getVisualIndicatorManager().forceRefreshIndicators(targetPlayer);
														LOGGER.info("Refreshed visual indicators for " + targetPlayer.getName() + " after bounty update");
													}

													// Send success message
													this.i18n(
															"bounty_creation.confirm.success",
															player
													).includePrefix().withPlaceholder(
															"target_name",
															target.map(OfflinePlayer::getName).orElse("not_defined")
													).build().sendMessage();

													LOGGER.info("Sent success message to " + player.getName() + " for bounty update");
												});
											})
											.exceptionally(throwable -> {
												LOGGER.log(Level.SEVERE, "Failed to add rewards to bounty for " + target.get().getName(), throwable);

												// Send error message on main thread
												rdq.getPlatform().getScheduler().runSync(() -> {
													this.i18n(
															"bounty_creation.confirm.error",
															player
													).includePrefix().withPlaceholder(
															"error_message",
															"Failed to add rewards to bounty: " + throwable.getMessage()
													).build().sendMessage();
												});
												return null;
											});
								});
							} else {
								rdq
										.getBountyFactory()
										.createBounty(
												target.get().getUniqueId(),
												player.getUniqueId(),
												this.mergeSimilarRewardItems(this.rewards.get(clickContext))
										)
										.thenAccept(createdBounty -> {
											rdq.getPlatform().getScheduler().runSync(() -> {
												Player targetPlayer = Bukkit.getPlayer(target.get().getUniqueId());
												if (targetPlayer != null && targetPlayer.isOnline()) {
													rdq.getVisualIndicatorManager().applyIndicators(targetPlayer);
													rdq.getVisualIndicatorManager().updatePlayerDisplay(targetPlayer);
												}

												this.i18n(
														"bounty_creation.confirm.success",
														player
												).includePrefix().withPlaceholder(
														"target_name",
														target.map(OfflinePlayer::getName).orElse("not_defined")
												).build().sendMessage();
											});
										})
										.exceptionally(ex -> {
											this.i18n(
													"confirm.error",
													player
											).includePrefix().withPlaceholder(
													"error_message",
													"Failed to create bounty: " + ex.getMessage()
											).build().sendMessage();
											return null;
										});
								
								// Cleanup for update path
								this.insertedItems.get(clickContext).remove(clickContext.getPlayer().getUniqueId());
								this.isReopening = false;
							}
							
							// Cleanup for creation path
							this.insertedItems.get(clickContext).remove(clickContext.getPlayer().getUniqueId());
							this.isReopening = false;
						},
						rdq.getExecutor()
					)
					.exceptionally(throwable -> {
						LOGGER.log(
							Level.WARNING,
							"Error occurred when trying to search for an existing bounty: " + throwable.getMessage()
						);
						rdq.getPlatform().getScheduler().runSync(clickContext::closeForPlayer);
						return null;
					});
			})
			.closeOnClick()
		;
	}
	
	/**
	 * Handles logic when the view is closed, refunding any inserted items if not reopening.
	 *
	 * @param close the close context
	 */
	@Override
	public void onClose(@NotNull CloseContext close) {
		
		if (
			this.isReopening
		) {
			return;
		}
		
		refundInsertedItems(
			close.getPlayer(),
			this.insertedItems
				.get(close)
				.containsKey(close
					             .getPlayer()
					             .getUniqueId()) ?
			this.insertedItems
				.get(close)
				.get(close
					     .getPlayer()
					     .getUniqueId())
				.values() :
			new ArrayList<>()
		);
	}
	
	/**
	 * Refunds any items inserted by the player into the GUI, returning them to the player's inventory or dropping them if full.
	 *
	 * @param player the player to refund items to
	 * @param items  the collection of items to refund
	 */
	private void refundInsertedItems(
		final @NotNull Player player,
		final @Nullable Collection<ItemStack> items
	) {
		
		if (
			items == null ||
			items.isEmpty()
		) {
			return;
		}
		
		player
			.getInventory()
			.addItem(items.toArray(new ItemStack[0]))
			.forEach((i, item) -> player
				                      .getWorld()
				                      .dropItem(
					                      player
						                      .getLocation()
						                      .clone()
						                      .add(
							                      0,
							                      0.5,
							                      0
						                      ),
					                      item
				                      ))
		;
		
		this.i18n(
			"left_overs",
			player
		).includePrefix().build().sendMessage();
	}

	private List<BountyReward> mergeSimilarRewardItems(
		final @NotNull List<BountyReward> items
	) {
		
		LOGGER.info("[BountyCreationView] mergeSimilarRewardItems called with " + items.size() + " items");
		for (int i = 0; i < items.size(); i++) {
			BountyReward item = items.get(i);
			if (item.getReward() instanceof ItemReward) {
				ItemReward itemReward = (ItemReward) item.getReward();
				LOGGER.info("[BountyCreationView] Input item " + i + ": " + itemReward.getItem().getType() + " x" + itemReward.getItem().getAmount());
			}
		}
		
		List<BountyReward> merged = new ArrayList<>();

		outer:
		for (BountyReward bountyReward : items) {
			if (!(bountyReward.getReward() instanceof ItemReward)) {
				merged.add(bountyReward); // Add non-item rewards directly
				continue;
			}

			ItemReward currentItemReward = (ItemReward) bountyReward.getReward();
			ItemStack currentItem = currentItemReward.getItem();
			
			LOGGER.info("[BountyCreationView] Processing item: " + currentItem.getType() + 
			                  " with ItemStack.getAmount()=" + currentItem.getAmount());
			
            for (BountyReward existing : merged) {
				if (existing.getReward() instanceof ItemReward) {
					ItemReward existingItemReward = (ItemReward) existing.getReward();
					ItemStack existingItem = existingItemReward.getItem();
					
					if (existingItem.isSimilar(currentItem)) {
						// Merge the amounts - just add the ItemStack amounts
						int existingAmount = existingItem.getAmount();
						int currentAmount = currentItem.getAmount();
						int totalAmount = existingAmount + currentAmount;
						
						LOGGER.info("[BountyCreationView] Merging in UI: " + existingAmount + " (existing) + " + currentAmount + " (current) = " + totalAmount);
						
						// Update the ItemStack amount
						existingItem.setAmount(totalAmount);
						LOGGER.info("[BountyCreationView] Final UI merge amount: " + existingItem.getAmount());
						continue outer;
					}
				}
			}
			merged.add(bountyReward);
		}

		return new ArrayList<>(merged);
	}
	
}
