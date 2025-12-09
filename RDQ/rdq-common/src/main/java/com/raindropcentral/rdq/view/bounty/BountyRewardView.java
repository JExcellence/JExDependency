package com.raindropcentral.rdq.view.bounty;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.bounty.Bounty;
import com.raindropcentral.rdq.reward.ItemReward;
import com.raindropcentral.rdq.reward.Reward;
import com.raindropcentral.rplatform.utility.heads.view.Proceed;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
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

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class BountyRewardView extends APaginatedView<ItemReward> {
	
	private final State<RDQ>                                rdq              = initialState("plugin");
	private final MutableState<Optional<OfflinePlayer>>     target           = initialState("target");
	private final MutableState<List<ItemReward>>             rewards      = initialState("rewards");
	private final State<Optional<Bounty>>                  bounty           = initialState("bounty");
	private final State<Map<UUID, Map<Integer, ItemStack>>> insertedItems    = initialState("insertedItems");
	
	private boolean isReturning;
	
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
	protected CompletableFuture<List<ItemReward>> getAsyncPaginationSource(
		final @NotNull Context context
	) {
		
		if (
			this.bounty.get(context).isEmpty() ||
			this.bounty.get(context).get().getRewards().isEmpty()
		) {
			ItemReward pseudoItem = new ItemReward(
					this.buildPane(
							Material.GRAY_STAINED_GLASS_PANE,
							context.getPlayer(),
							"bounty_reward_ui.pseudo.name",
							"bounty_reward_ui.pseudo.lore"
					),
					0
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
			this.bounty.get(context).get().getRewards().stream().filter(bountyReward -> bountyReward.getReward().getType().equals(Reward.Type.ITEM)).map(bountyReward -> (ItemReward) bountyReward.getReward()).toList()
		);
	}
	
	@Override
	protected void renderEntry(
		final @NotNull Context context,
		final @NotNull BukkitItemComponentBuilder builder,
		final int index,
		final @NotNull ItemReward rewardItem
	) {
		
		if (
			this.bounty.get(context).isEmpty()
		) {
			builder
				.renderWith(() -> UnifiedBuilderFactory.item(
						rewardItem.getItem()
				).build())
				.updateOnStateChange(this.bounty);
			return;
		}
		this
			.splitToMaxStacks(rewardItem)
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
								                  .splitLines()
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
					"bounty_reward_ui.input_slot.name",
					"bounty_reward_ui.input_slot.lore"
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
				.head()
				.setPlayerHead(target.getPlayer())
				.setName(
					this.i18n(
						    "target.name",
						    player
					    ).with(
						    "target_name",
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
					).withAll(
						Map.of(
							"target_name",
							target.getName() == null ?
							"" :
							target.getName()
						)
					).build().splitLines()
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
					Set<ItemReward> rewards = new HashSet<>();
					for (
						ItemStack stack : playerSlots.values()
					) {
						rewards.add(new ItemReward(
								stack,
								stack.getAmount()
						));
					}
					
					this.rewards.get(clickContext).addAll(rewards);
					this.isReturning = true;
					
					clickContext.openForPlayer(
						BountyCreationView.class,
						clickContext.getInitialData()
					);
				} else {
					this.i18n(
						"no_new_items_inserted",
						player
					).withPrefix().send();
				}
			});
	}
	
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
				close.getInitialData()
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
							"bounty_reward_ui.input_slot.name",
							"bounty_reward_ui.input_slot.lore"
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
						   TranslationService.create(
					       TranslationKey.of(nameKey),
					       player
				       ).build().component()
			       )
			       .setLore(
				       TranslationService.create(
					       TranslationKey.of(loreKey),
					       player
				       ).build().splitLines()
			       )
			       .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
			       .build();
	}
	
	private List<ItemStack> splitToMaxStacks(
		final @NotNull ItemReward rewardItem
	) {
		
		List<ItemStack> result   = new ArrayList<>();
		ItemStack       base     = rewardItem.getItem();
		int             total    = rewardItem.getItem().getAmount();
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
		    .withPrefix()
		    .send();
		;
	}
	
}