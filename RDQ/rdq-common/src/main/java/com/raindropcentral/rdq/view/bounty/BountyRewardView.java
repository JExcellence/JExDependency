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

package com.raindropcentral.rdq.view.bounty;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.bounty.Bounty;
import com.raindropcentral.rdq.database.entity.bounty.BountyReward;
import com.raindropcentral.rplatform.reward.impl.ItemReward;
import com.raindropcentral.rplatform.utility.heads.view.Proceed;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.CloseContext;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.MutableState;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Represents the BountyRewardView API type.
 */
public class BountyRewardView extends APaginatedView<BountyReward> {
	
	private final State<RDQ>                                rdq              = initialState("plugin");
	private final MutableState<Optional<OfflinePlayer>>     target           = initialState("target");
	private final MutableState<List<BountyReward>>             rewards      = initialState("rewards");
	private final State<Optional<Bounty>>                  bounty           = initialState("bounty");
	private final State<Map<UUID, Map<Integer, ItemStack>>> insertedItems    = initialState("insertedItems");
	
	private boolean isReturning;
	
	/**
	 * Executes BountyRewardView.
	 */
	public BountyRewardView() {
		super(BountyCreationView.class);
	}
	
	@Override
	protected String getKey() {
		
		return "bounty_reward_ui";
	}
	
	@Override
	protected String[] getLayout() {
		
		return new String[]{
			"    t    ",
			"< OOOOO >",
			"         ",
			" xxxxxxx ",
			"         ",
			"b       c"
		};
	}
	
	@Override
	protected CompletableFuture<List<BountyReward>> getAsyncPaginationSource(
		final @NotNull Context context
	) {
		
		if (
			this.bounty.get(context).isEmpty() ||
			this.bounty.get(context).get().getRewards().isEmpty()
		) {
			BountyReward pseudoItem = new BountyReward(
					new ItemReward(this.buildPane(
							Material.GRAY_STAINED_GLASS_PANE,
							context.getPlayer(),
							"pseudo.name",
							"pseudo.lore"
					)
					),
					UUID.randomUUID()
			);

			return CompletableFuture.completedFuture(List.of(
				pseudoItem,
				pseudoItem,
				pseudoItem,
				pseudoItem,
				pseudoItem
			));
		}
		return CompletableFuture.completedFuture(
			this.bounty.get(context).get().getRewards().stream().filter(bountyReward -> bountyReward.getReward() instanceof ItemReward).toList()
		);
	}
	
	@Override
	protected void renderEntry(
		final @NotNull Context context,
		final @NotNull BukkitItemComponentBuilder builder,
		final int index,
		final @NotNull BountyReward bountyReward
	) {
		ItemReward itemReward = ((ItemReward) bountyReward.getReward());
		
		if (
			this.bounty.get(context).isEmpty()
		) {
			builder
				.renderWith(() -> UnifiedBuilderFactory.item(
						itemReward.getItem()
				).build())
				.updateOnStateChange(this.bounty);
			return;
		}
		this
			.splitToMaxStacks(itemReward)
			.forEach(
				item -> builder
					        .renderWith(
						        () -> UnifiedBuilderFactory
							              .item(item.clone())
							              .setName(item.clone().displayName())
							              .setLore(
								              this.i18n(
									                  "reward_item.lore",
									                  context.getPlayer()
								                  )
								                  .build()
								                  .children()
							              )
							              .addLoreLines(
								              item.lore() == null ?
								              new ArrayList<>() :
								              Objects.requireNonNull(item.lore())
							              )
							              .build()
					        )
					        .updateOnStateChange(this.bounty));
	}
	
	@Override
	protected void onPaginatedRender(
		final @NotNull RenderContext render,
		final @NotNull Player player
	) {

		final OfflinePlayer target = this.target.get(render).orElse(null);

		render
			.layoutSlot(
				'x',
				buildPane(
					Material.GREEN_STAINED_GLASS_PANE,
					player,
					"input_slot.name",
					"input_slot.lore"
				)
			)
			.onClick(this::handleSlotClick);
		
		if (
			this.insertedItems.get(render).containsKey(player.getUniqueId()) &&
			! this.insertedItems.get(render).get(player.getUniqueId()).isEmpty()
		) {
			this.insertedItems
				.get(render)
				.get(player.getUniqueId())
				.forEach(
					(slot, item) -> render.slot(
						slot,
						item
					).onClick(this::handleSlotClick)
				)
			;
		}
		
		render.layoutSlot(
			't',
			UnifiedBuilderFactory
				.unifiedHead(target.getPlayer())
				.setDisplayName(
					(net.kyori.adventure.text.Component) this.i18n(
						    "target.name",
						    player
					    ).withPlaceholder("target_name",
						    target.getName() == null ?
						    "" :
						    target.getName()
					    )
					    .build()
					    .component()
				)
				.setLore(
					this.i18n(
						"target.lore",
						player
					).withPlaceholders(
						Map.of(
							"target_name",
							target.getName() == null ?
							"" :
							target.getName()
						)
					).build().children()
				)
				.build()
		);
		
		render
			.layoutSlot(
				'c',
				new Proceed().getHead(player)
			)
			.updateOnStateChange(this.insertedItems)
			.onClick(clickContext -> {
				Map<Integer, ItemStack> playerSlots = this.insertedItems.get(render).get(player.getUniqueId());
				
				if (
					playerSlots != null &&
					! playerSlots.isEmpty()
				) {
					List<BountyReward> newRewards = new ArrayList<>();
					for (
						ItemStack stack : playerSlots.values()
					) {
						// Create a clean ItemStack with amount=1 for the ItemReward constructor
						ItemStack template = stack.clone();
						int originalAmount = template.getAmount();
						
						// Ensure the template has amount=1 before passing to constructor
						template.setAmount(1);
						
						// Create the ItemReward with explicit amount (can exceed max stack size)
						ItemReward itemReward = new ItemReward(template, originalAmount);
						
						newRewards.add(new BountyReward(
								itemReward,
								player.getUniqueId()
						));
					}
					
					// Only add the new rewards that were just inserted, don't accumulate with existing ones
					// The existing rewards are already in the bounty, we just want to add the new ones
					this.rewards.get(clickContext).clear();
					this.rewards.get(clickContext).addAll(newRewards);
					this.isReturning = true;
					
					clickContext.openForPlayer(
						BountyCreationView.class,
						Map.of(
								"plugin",
								rdq.get(clickContext),
								"target",
								this.target.get(render),
								"rewards",
								this.rewards.get(clickContext),
								"bounty",
								bounty.get(clickContext),
								"insertedItems",
								insertedItems.get(clickContext)
						)
					);
				} else {
					this.i18n(
						"no_new_items_inserted",
						player
					).includePrefix().build().sendMessage();
				}
			});
	}
	
	/**
	 * Executes onClick.
	 */
	@Override
	public void onClick(
		final @NotNull SlotClickContext click
	) {
		if (
			click.isShiftClick() &&
			click.getClickedContainer().isEntityContainer()
		) {
			this.handleShiftClick(click);
			return;
		}

		if (
			! click.isShiftClick() &&
			click.getClickedContainer().isEntityContainer()
		) {
			click.setCancelled(false);
		}
	}
	
	/**
	 * Executes onClose.
	 */
	@Override
	public void onClose(
		final @NotNull CloseContext close
	) {
		
		if (
			this.isReturning
		) {
			return;
		}
		
		if (
			this.insertedItems.get(close).containsKey(close.getPlayer().getUniqueId())
		) {
			refundInsertedItems(
				close.getPlayer(),
				this.insertedItems.get(close).get(close.getPlayer().getUniqueId()).values()
			);
			
			this.insertedItems.get(close).remove(close.getPlayer().getUniqueId());
		}
		
		this.rdq
			.get(close)
			.getViewFrame()
			.open(
				BountyCreationView.class,
				close.getPlayer(),
					Map.of(
							"plugin",
							rdq.get(close),
							"target",
							this.target.get(close),
							"rewards",
							this.rewards.get(close),
							"bounty",
							bounty.get(close),
							"insertedItems",
							insertedItems.get(close)
					)
			)
		;
	}
	
	private void handleSlotClick(
		final @NotNull SlotClickContext clickContext
	) {
		
		final ItemStack cursorItem      = clickContext.getClickOrigin().getCursor();
		final int       clickedSlot     = clickContext.getClickedSlot();
		final ItemStack currentSlotItem = clickContext.getClickOrigin().getCurrentItem();
		
		boolean isSlotEmptyOrGreenPane =
			currentSlotItem == null
			|| currentSlotItem.getType() == Material.AIR
			|| currentSlotItem.getType() == Material.GREEN_STAINED_GLASS_PANE;
		
		Map<Integer, ItemStack> playerSlots = this.insertedItems.get(clickContext).computeIfAbsent(
			clickContext
				.getPlayer()
				.getUniqueId(),
			k -> new HashMap<>()
		);
		
		if (
			clickContext.getClickedContainer().isEntityContainer() &&
			clickContext.isShiftClick()
		) {
			clickContext.setCancelled(true);
			return;
		}
		
		if (
			clickContext.isLeftClick()
		) {
			if (
				isSlotEmptyOrGreenPane &&
				cursorItem.getType() != Material.AIR
			) {
				clickContext.getClickOrigin().setCursor(null);
				playerSlots.put(
					clickedSlot,
					cursorItem.clone()
				);
				clickContext
					.getClickedContainer()
					.renderItem(
						clickedSlot,
						cursorItem
					);
			}
			return;
		}
		
		if (
			clickContext.isRightClick()
		) {
			if (
				! isSlotEmptyOrGreenPane &&
				currentSlotItem.getType() != Material.AIR
			) {
				ItemStack removed = playerSlots.remove(clickedSlot);
				if (
					removed != null
				) {
					refundInsertedItems(
						clickContext.getPlayer(),
						List.of(removed)
					);
				}
				clickContext
					.getClickedContainer()
					.renderItem(
						clickedSlot,
						buildPane(
							Material.GREEN_STAINED_GLASS_PANE,
							clickContext.getPlayer(),
							"input_slot.name",
							"input_slot.lore"
						)
					);
			}
		}
	}
	
	private void handleShiftClick(
		final @NotNull SlotClickContext click
	) {
		
		final Player    player      = click.getPlayer();
		final ItemStack clickedItem = click.getClickOrigin().getCurrentItem();
		
		if (
			clickedItem != null &&
			clickedItem.getType() != Material.AIR
		) {
			Inventory guiInv = player.getOpenInventory().getTopInventory();
			int targetSlot = findFirstPaneSlot(
				guiInv,
				Set.of(
					Material.LIME_STAINED_GLASS_PANE,
					Material.GREEN_STAINED_GLASS_PANE
				)
			);
			if (
				targetSlot != - 1
			) {
				player

					.getInventory()
					.removeItem(clickedItem);
				guiInv.setItem(
					targetSlot,
					clickedItem.clone()
				);
				this.insertedItems
					.get(click)
					.computeIfAbsent(
						player.getUniqueId(),
						k -> new HashMap<>()
					)
					.put(
						targetSlot,
						clickedItem.clone()
					)
				;
				click.setCancelled(true);
				return;
			}
		}
		click.setCancelled(true);
	}
	
	private int findFirstPaneSlot(
		final @NotNull Inventory inv,
		final @NotNull Set<Material> paneTypes
	) {
		
		for (
			int i = 0; i < inv.getSize(); i++
		) {
			ItemStack slotItem = inv.getItem(i);
			if (slotItem != null && paneTypes.contains(slotItem.getType())) {
				return i;
			}
		}
		return - 1;
	}
	
	private ItemStack buildPane(
		final @NotNull Material paneType,
		final @NotNull Player player,
		final @NotNull String nameKey,
		final @NotNull String loreKey
	) {
		
		return UnifiedBuilderFactory
			       .item(paneType)
			       .setName(
						   new I18n.Builder(nameKey, player).build().component()
			       )
			       .setLore(
				       new I18n.Builder(loreKey, player).build().children()
			       )
			       .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
			       .build();
	}
	
	private List<ItemStack> splitToMaxStacks(
		final @NotNull ItemReward rewardItem
	) {
		
		List<ItemStack> result   = new ArrayList<>();
		ItemStack       base     = rewardItem.getItem();
		int             total    = base.getAmount(); // Get amount from ItemStack
		int             maxStack = base.getMaxStackSize();
		
		while (
			total > 0
		) {
			int stackAmount = Math.min(
				total,
				maxStack
			);
			ItemStack stack = base.clone();
			stack.setAmount(stackAmount);
			result.add(stack);
			total -= stackAmount;
		}
		return result;
	}
	
	private void refundInsertedItems(
		final @NotNull Player player,
		final @NotNull Collection<ItemStack> items
	) {
		
		if (
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
		    )
		    .includePrefix()
		    .build().sendMessage();
		;
	}
	
}
