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
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;

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
	
	/**
	 * The com.raindropcentral.rdq.RDQ plugin instance.
	 */
	private final State<RDQ> rdq = initialState("plugin");
	
	/**
	 * The currently selected target player for the bounty (if any).
	 */
	private final MutableState<Optional<OfflinePlayer>> target = initialState("target");
	
	/**
	 * The set of item rewards being offered for the bounty.
	 */
	private final MutableState<Set<BountyReward>> rewards = initialState("rewards");
	
	/**
	 * The current bounty being edited or viewed (if any).
	 */
	private final State<Optional<Bounty>> bounty = initialState("bounty");
	
	/**
	 * Tracks items inserted by players into the GUI, mapped by player UUID and slot index.
	 */
	private final State<Map<UUID, Map<Integer, ItemStack>>> insertedItems = initialState("insertedItems");
	
	/**
	 * Indicates whether the view is being reopened (e.g., after navigating to a subview).
	 */
	private boolean isReopening;
	
	/**
	 * Computed state for the "Select Target" button, displaying the selected player or a prompt.
	 */
	private final State<ItemStack> targetSelectorButton = computedState(context -> {
		Player                  player       = context.getPlayer();
		Optional<OfflinePlayer> targetPlayer = this.target.get(context);
		String                  targetName   = targetPlayer.map(OfflinePlayer::getName).orElse("");
		return UnifiedBuilderFactory
			       .head()
			       .setPlayerHead(targetPlayer.orElse(null))
			       .setName(
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
			       .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
			       .build();
	});
	
	/**
	 * Computed state for the "Add Items" button, enabled only if a target is selected.
	 */
	private final State<ItemStack> itemAdderButton = computedState(context -> {
		Player  player  = context.getPlayer();
		boolean enabled = this.target.get(context).isPresent();
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
					       this.rewards.get(context).stream().filter(bountyReward -> bountyReward.getReward().getType().equals(Reward.Type.ITEM)).toList().size()
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
							   this.rewards.get(context).stream().filter(bountyReward -> bountyReward.getReward().getType().equals(Reward.Type.CURRENCY)).toList().size()
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
	protected int getUpdateSchedule() {
		
		return 20;
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
			.slot(13)
			.watch(this.itemAdderButton)
			.renderWith(() -> this.itemAdderButton.get(render))
			.displayIf(() -> this.target
				                 .get(render)
				                 .isPresent())
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
					Map<String, Object> data = new HashMap<>((Map<String, Object>) context.getInitialData());
					
					data.remove("bounty");
					data.remove("target");
					
					data.put(
						"bounty",
						this.bounty.get(context).get()
					);
					
					data.put(
						"target",
						target.get(context).get()
					);
					
					context.openForPlayer(
						BountyPlayerInfoView.class,
						data
					);
					return;
				}
				
				var foundBounty = this.rdq.get(context).getBountyRepository().findByAttributes(
					Map.of(
						"player.uniqueId",
						this.target.get(context).get().getUniqueId()
					)
				);
				
				if (foundBounty != null) {
					var data = (Map<String, Object>) context.getInitialData();
					
					data.remove("bounty");
					
					Optional<OfflinePlayer> target = (Optional<OfflinePlayer>) data.get("target");
					
					data.remove("target");
					
					data.put(
						"bounty",
						foundBounty
					);
					
					data.put(
						"target",
						target.get()
					);
					
					context.setInitialData(data);
					
					context.openForPlayer(
						BountyPlayerInfoView.class,
						context.getInitialData()
					);
				} else {
					context.openForPlayer(
						BountyRewardView.class,
						context.getInitialData()
					);
				}
			})
		;
		
		render
			.slot(15)
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
							"player.uniqueId",
							target
								.get()
								.getUniqueId()
						)
					)
					.whenCompleteAsync(
						(bounty, throwable) -> {
							if (
								throwable != null
							) {
								CentralLogger.getLogger(BountyCreationView.class.getName()).log(
									Level.WARNING,
									"Error occurred, when trying to search for an existing bounty: " + throwable.getMessage()
								);
								clickContext.closeForPlayer();
								return;
							}
							
							if (
								bounty != null
							) {
								clickContext.openForPlayer(
									BountyPlayerInfoView.class,
									clickContext.getInitialData()
								);
							}
						}
					)
					.thenAcceptAsync(
						bounty -> {
							rdq
								.getPlayerRepository()
								.findByAttributesAsync(
									Map.of(
										"player.uniqueId",
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
										
										if (
											bounty == null
										) {
											rdq
												.getBountyFactory()
												.createBounty(
													rdqPlayer.getUniqueId(),
													target.get().getUniqueId(),
													this.mergeSimilarRewardItems(this.rewards.get(clickContext))
												);
										} else {
											rdq
												.getBountyRepository()
												.update(
													bounty
												);
											
											this.insertedItems.get(clickContext).remove(clickContext.getPlayer().getUniqueId());
											this.isReopening = false;
										}
										
										this.i18n(
											"confirm.success",
											player
										).withPrefix().with(
											"target_name",
											target.map(OfflinePlayer::getName).orElse("not_defined")
										).send();
										this.insertedItems.get(clickContext).remove(clickContext.getPlayer().getUniqueId());
										this.isReopening = false;
									},
									rdq.getExecutor()
								)
							;
						},
						rdq.getExecutor()
					)
				;
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
	
	/**
	 * Merges similar {@link RewardItem}s (same item type and meta) into one with the total amount.
	 * The contributor and contributedAt fields will be taken from the first occurrence.
	 *
	 * @param items the set of reward items to merge
	 *
	 * @return a new set of merged reward items
	 */
	private List<BountyReward> mergeSimilarRewardItems(
		final @NotNull Set<BountyReward> items
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