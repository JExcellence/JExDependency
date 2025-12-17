package com.raindropcentral.rdq.view.bounty;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.bounty.Bounty;
import com.raindropcentral.rdq.database.entity.bounty.BountyReward;
import com.raindropcentral.rdq.reward.ItemReward;
import com.raindropcentral.rdq.reward.Reward;
import com.raindropcentral.rplatform.utility.heads.view.Cancel;
import com.raindropcentral.rplatform.utility.heads.view.Return;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import com.raindropcentral.rplatform.view.ConfirmationView;
import me.devnatan.inventoryframework.context.Context;
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

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * View for displaying detailed information about a specific bounty and its target player.
 * <p>
 * This view allows users to:
 * <ul>
 *     <li>See the target player's head and information</li>
 *     <li>View the bounty's rewards</li>
 *     <li>Navigate back to the main bounty menu</li>
 *     <li>Delete the bounty (if the user is an operator), with confirmation</li>
 * </ul>
 * <p>
 * Integrates with the organization's unified item builder, i18n, and confirmation view systems.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class BountyPlayerInfoView extends BaseView {

	private final State<RDQ>                                rdq              = initialState("plugin");
	private final MutableState<Optional<OfflinePlayer>> target           = initialState("target");
	private final MutableState<List<BountyReward>>             rewards      = initialState("rewards");
	private final State<Optional<Bounty>>                  bounty           = initialState("bounty");
	private final State<Map<UUID, Map<Integer, ItemStack>>> insertedItems    = initialState("insertedItems");
	
	@Override
	protected String[] getLayout() {
		
		return
			new String[]{
				"         ",
				"    p    ",
				"  i   r  ",
				"b       d"
			};
	}
	
	@Override
	protected int getSize() {
		
		return 4;
	}
	
	@Override
	protected Map<String, Object> getTitlePlaceholders(
		final @NotNull OpenContext open
	) {
		
		return Map.of(
			"target_name",
				Bukkit.getOfflinePlayer(this.bounty.get(open).get().getTargetUniqueId()).getName()
		);
	}
	
	@Override
	protected String getKey() {
		
		return "bounty_player_info_ui";
	}
	
	@Override
	public void onFirstRender(
		final @NotNull RenderContext render,
		final @NotNull Player player
	) {
		
		render.layoutSlot(
			'p',
			UnifiedBuilderFactory
				.unifiedHead(this.target.get(render).get())
				.setDisplayName(
					(net.kyori.adventure.text.Component) this.i18n(
						    "target.name",
						    player
					    )
					    .withPlaceholder("player_name",
						    player.getName()
					    )
					    .build()
					    .component()
				)
				.build()
		);
		
		render.layoutSlot(
			'i',
			UnifiedBuilderFactory
				.unifiedHead()
				.build()
			//TODO
		);

		List<BountyReward> bountyRewards = new ArrayList<>(this.bounty.get(render).get().getRewards());
		bountyRewards = bountyRewards.stream().filter(bountyReward -> bountyReward.getReward().getType().equals(Reward.Type.ITEM)).toList();
		
		render
			.layoutSlot(
				'r',
				UnifiedBuilderFactory
					.item(Material.CHEST)
					.setName(
						this.i18n(
							"rewards.name",
							player
						).build().component()
					)
					.setLore(
						this.i18n(
							    "rewards.lore",
							    player
						    )
						    .withPlaceholders(
							    Map.of(
								    "item_amount",
										bountyRewards.stream().mapToInt(bountyReward -> ((ItemReward) bountyReward.getReward()).getItem().getAmount()),
								    "item_list",
								    String.join(", ", bountyRewards.stream().map(bountyReward -> bountyReward.getIcon().getDescriptionKey()).toList())
							    )
						    )
						    .build()
						    .children()
					)
					.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
					.build()
			)
			.onClick(
				clickContext -> clickContext.openForPlayer(
					BountyRewardView.class,
					Map.of(
						"plugin",
						rdq.get(clickContext),
						"target",
						target.get(clickContext),
						"rewards",
						bounty.get(clickContext).get().getRewards(),
						"bounty",
							bounty.get(clickContext),
						"insertedItems",
						new HashMap<>()
					)
				)
			);
		
		render
			.layoutSlot(
				'b',
				new Return().getHead(player)
			)
			.onClick(clickContext -> clickContext.openForPlayer(
				BountyMainView.class,
				Map.of(
					"plugin",
					rdq.get(clickContext),
					"target",
					target.get(clickContext),
					"rewards",
					this.bounty.get(clickContext).get().getRewards(),
					"bounty",
					this.bounty.get(clickContext),
					"insertedItems",
					new HashMap<>()
				)
			));
		
		render
			.layoutSlot(
				'd',
				UnifiedBuilderFactory.item(new Cancel().getHead(player)).setName(
					this.i18n(
						"delete.name",
						player
					).build().component()
				).setLore(
					this.i18n(
						"delete.lore",
						player
					).build().children()
				).build()
			).displayIf(
				context -> context.getPlayer().isOp()
			).onClick(clickContext ->
				new ConfirmationView.Builder()
						.withKey("bounty_player_info_ui")
								.withMessageKey("bounty_player_info_ui.confirm.message")
										.withInitialData(
												Map.of(
												"plugin",
												rdq.get(clickContext),
												"target",
												this.target.get(render),
												"rewards",
												new ArrayList<>(),
												"bounty",
												bounty.get(clickContext),
												"insertedItems",
												new HashMap<>()
												)
										).withCallback(
												confirmationResult -> {
													if (confirmationResult) {
														rdq.get(clickContext).getBountyFactory().deleteBounty(this.bounty.get(clickContext).get());
														player.closeInventory();
													}

													else {
														//todo nothing
													}
												}
						).withParentView(BountyPlayerInfoView.class).openFor(clickContext, player)
			);
	}
	
	/**
	 * Handles the result of the confirmation dialog when attempting to delete a bounty.
	 * If confirmed, deletes the bounty from the repository and removes it from the active bounties.
	 * Sends a success message to the player and closes the view.
	 *
	 * @param origin the context from which the resume was triggered
	 * @param target the context to which the resume is applied
	 */
	@Override
	public void onResume(
		@NotNull final Context origin,
		@NotNull final Context target
	) {
		
		Map<String, Object> initialData = (Map<String, Object>) target.getInitialData();
		
		if (
			initialData == null ||
			initialData.get("confirmed") == null ||
			! initialData.get("confirmed").equals(true)
		) {
			return;
		}
		
		var bounty = (Optional<Bounty>) initialData.get("bounty");
		if (
			bounty.isEmpty()
		) {
			return;
		}
		
		var targetPlayer = (Optional<OfflinePlayer>) initialData.get("target");
		
		final RDQ rdq = (RDQ) initialData.get("plugin");
		
		var commissionerUniqueId = bounty.get().getCommissionerUniqueId();

		CompletableFuture.supplyAsync(
				() -> rdq.getPlayerRepository().findByAttributes(Map.of("uniqueId", commissionerUniqueId)).orElse(null),
				rdq.getExecutor()
		).thenCompose(rdqPlayer -> {
			if (rdqPlayer == null) {
				return null;
			}

			rdq.getPlayerRepository().updateAsync(
					rdqPlayer
			).thenAcceptAsync(
					player -> {
						rdq.getBountyRepository().deleteAsync(bounty.get().getId()).thenAccept(
								(v) -> {
									rdq.getVisualIndicatorManager().removeIndicators(targetPlayer.get().getUniqueId());

									i18n("deleted_bounty_successfully", origin.getPlayer()
									).includePrefix().withPlaceholders(
											Map.of(
													"bounty_id",
													bounty.get().getId(),
													"target_name",
													targetPlayer.get().getName(),
													"target_uniqueId",
													targetPlayer.get().getUniqueId()
											)
									).build().sendMessage();
								}
						);
					},
					rdq.getExecutor()
			);
			return null;
		});
		
		target.openForPlayer(
			BountyMainView.class,
				Map.of(
						"plugin",
						rdq,
						"target",
						targetPlayer,
						"rewards",
						rewards,
						"bounty",
						this.bounty.get(target),
						"insertedItems",
						insertedItems
				)
		);
	}
	
}