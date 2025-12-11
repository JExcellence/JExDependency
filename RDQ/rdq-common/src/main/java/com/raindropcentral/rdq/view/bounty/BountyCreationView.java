package com.raindropcentral.rdq.view.bounty;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.bounty.Bounty;
import com.raindropcentral.rdq.database.entity.bounty.BountyReward;
import com.raindropcentral.rdq.reward.ItemReward;
import com.raindropcentral.rdq.reward.Reward;
import com.raindropcentral.rplatform.logging.CentralLogger;
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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * InventoryFramework view for creating a new bounty in the RaindropQuests system.
 * <p>
 * This view allows players to:
 * <ul>
 *   <li>Select a target player for the bounty</li>
 *   <li>Add item and currency rewards</li>
 *   <li>Confirm and submit the bounty</li>
 * </ul>
 * The view manages state for the selected target, rewards, and inserted items,
 * and provides feedback and validation throughout the bounty creation process.
 * </p>
 *
 * <p>
 * Navigation to related views (such as {@link PaginatedPlayerView}, {@link BountyRewardView}, and {@link BountyPlayerInfoView})
 * is handled based on user actions and current state.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class BountyCreationView extends BaseView {

	private static final Logger LOGGER = Logger.getLogger(BountyCreationView.class.getName());

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
				       this.i18n(
					       "select_target.name",
					       player
				       ).with(
					       "target_name",
					       targetName
				       ).build().component()
			       )
			       .setLore(
				       this.i18n(
					       "select_target.lore",
					       player
				       ).with(
					       "target_name",
					       targetName
				       ).build().splitLines()
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
		bountyRewards = bountyRewards.stream().filter(bountyReward -> bountyReward.getReward().getType().equals(Reward.Type.ITEM)).toList();
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
				       ).with(
					       "count",
					       bountyRewards.size()
				       ).build().splitLines()
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
		bountyRewards = bountyRewards.stream().filter(bountyReward -> bountyReward.getReward().getType().equals(Reward.Type.CURRENCY)).toList();
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
				       ).with(
					       "count",
							   bountyRewards.size()
				       ).build().splitLines()
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
				       ).build().splitLines()
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
					).withPrefix().send();
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
				
				if (foundBounty != null) {
					context.openForPlayer(
						BountyPlayerInfoView.class,
							Map.of(
									"plugin",
									rdq.get(context),
									"bounty",
									Optional.of(foundBounty),
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
					).withPrefix().send();
					return;
				}
				
				if (
					this.rewards.get(clickContext).isEmpty()
				) {
					this.i18n(
						"confirm.no_rewards_selected",
						player
					).withPrefix().send();
					return;
				}
				
				this.i18n(
					"confirm.confirmation",
					player
				).withPrefix().with(
					"target_name",
					this.target.get(clickContext).map(OfflinePlayer::getName).orElse("not_defined")
				).send();
				
				final RDQ                     rdq    = this.rdq.get(clickContext);
				final Optional<OfflinePlayer> target = this.target.get(clickContext);
				
				if (
					target.isEmpty()
				) {
					this.i18n(
						"confirm.error",
						player
					).withPrefix().with(
						"target_name",
						"not_defined"
					).send();
					return;
				}
				
				rdq
					.getBountyRepository()
					.findByAttributesAsync(
						Map.of(
							"targetUniqueId",
							target
								.get()
								.getUniqueId()
						)
					)
					.thenAcceptAsync(
						bounty -> {
							// Handle errors in the main logic
							if (bounty != null) {
								LOGGER.info("Found existing bounty for " + target.get().getName() + ", opening info view");
								rdq.getPlatform().getScheduler().runSync(() -> {
									clickContext.openForPlayer(
											BountyPlayerInfoView.class,
											Map.of(
													"plugin",
													rdq,
													"bounty",
													Optional.of(bounty),
													"target",
													target,
													"rewards",
													rewards.get(clickContext),
													"insertedItems",
													insertedItems.get(clickContext)
											)
									);
								});
								return;
							}
							rdq
								.getPlayerRepository()
								.findByAttributesAsync(
									Map.of(
										"uniqueId",
										target
											.get()
											.getUniqueId()
									)
								)
								.thenAcceptAsync(
									rdqPlayer -> {
										if (
											rdqPlayer == null
										) {
											this.i18n(
												"confirm.error",
												player
											).withPrefix().with(
												"target_name",
												"not_defined"
											).send();
											return;
										}

                                        // Create new bounty and apply visual indicators
                                        rdq
                                            .getBountyFactory()
                                            .createBounty(
                                                target.get().getUniqueId(),
                                                rdqPlayer.getUniqueId(),
                                                this.mergeSimilarRewardItems(this.rewards.get(clickContext))
                                            )
                                            .thenAccept(createdBounty -> {
												Bukkit.getScheduler().runTask(rdq.getPlugin(), () -> {
                                                // Apply visual indicators to the target player if they're online
                                                Player targetPlayer = Bukkit.getPlayer(target.get().getUniqueId());
                                                if (targetPlayer != null && targetPlayer.isOnline()) {
                                                    // Force immediate visual update
                                                    rdq.getVisualIndicatorManager().applyIndicators(targetPlayer);
                                                    rdq.getVisualIndicatorManager().updatePlayerDisplay(targetPlayer);
                                                    LOGGER.info("Applied visual indicators to " + targetPlayer.getName() + " for new bounty");
                                                } else {
                                                    LOGGER.info("Target player " + target.get().getName() + " is offline, indicators will be applied on join");
                                                }

                                                // Send success message

                                                    this.i18n(
                                                        "bounty_creation.confirm.success",
                                                        player
                                                    ).withPrefix().with(
                                                        "target_name",
                                                        target.map(OfflinePlayer::getName).orElse("not_defined")
                                                    ).send();
                                                });
                                            })
                                            .exceptionally(ex -> {
                                                // Send error message
                                                this.i18n(
                                                    "confirm.error",
                                                    player
                                                ).withPrefix().with(
                                                    "error_message",
                                                    "Failed to create bounty: " + ex.getMessage()
                                                ).send();
                                                return null;
                                            });
                                        this.insertedItems.get(clickContext).remove(clickContext.getPlayer().getUniqueId());
										this.isReopening = false;
									},
									rdq.getExecutor()
								)
							;
						},
						rdq.getExecutor()
					)
					.exceptionally(throwable -> {
						CentralLogger.getLogger(BountyCreationView.class.getName()).log(
							Level.WARNING,
							"Error occurred when trying to search for an existing bounty: " + throwable.getMessage()
						);
						Bukkit.getScheduler().runTask(rdq.getPlugin(), () -> {
							clickContext.closeForPlayer();
						});
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
		).withPrefix().send();
	}

	private List<BountyReward> mergeSimilarRewardItems(
		final @NotNull List<BountyReward> items
	) {
		
		List<BountyReward> merged = new ArrayList<>();

		outer:
		for (BountyReward bountyReward : items) {
			if (! bountyReward.getReward().getType().equals(Reward.Type.ITEM)) {
				continue;
			}

			ItemStack item = ((ItemReward) bountyReward.getReward()).getItem();
            for (
				BountyReward existing : merged
			) {
				if (((ItemReward) bountyReward.getReward()).getItem().isSimilar(item)) {
					((ItemReward) existing.getReward()).setAmount(((ItemReward) existing.getReward()).getAmount() + item.getAmount());
					continue outer;
				}
			}
			merged.add(bountyReward);
		}


		return new ArrayList<>(merged);
	}
	
}