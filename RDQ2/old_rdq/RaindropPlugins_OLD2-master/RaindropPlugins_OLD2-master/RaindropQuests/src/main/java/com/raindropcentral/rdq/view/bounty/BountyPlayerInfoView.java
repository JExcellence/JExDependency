package com.raindropcentral.rdq.view.bounty;

import com.raindropcentral.rdq.RDQImpl;
import com.raindropcentral.rdq.database.entity.RBounty;
import com.raindropcentral.rdq.database.entity.RDQPlayer;
import com.raindropcentral.rplatform.misc.heads.view.Cancel;
import com.raindropcentral.rplatform.misc.heads.view.Return;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.ConfirmationView;
import com.raindropcentral.rplatform.view.common.BaseView;

import de.jexcellence.translate.api.I18n;
import de.jexcellence.translate.api.MessageKey;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
	
	/**
	 * The RDQImpl plugin instance, used for accessing repositories and services.
	 */
	private final State<RDQImpl> rdq = initialState("plugin");
	
	/**
	 * The bounty being displayed in this view.
	 */
	private final State<RBounty> bounty = initialState("bounty");
	
	/**
	 * The target player of the bounty.
	 */
	private final State<OfflinePlayer> target = initialState("target");
	
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
			this.bounty.get(open).getPlayer().getPlayerName()
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
				.head()
				.setPlayerHead(this.target.get(render))
				.setName(
					this.i18n(
						    "target.name",
						    player
					    )
					    .withPlaceholder(
						    "player_name",
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
				.head()
				.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
				.build()
			//TODO
		);
		
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
								    this.bounty
									    .get(render)
									    .getRewardItems()
									    .stream()
									    .mapToInt(RewardItem::getAmount),
								    "item_list",
								    String.join(
									    ", ",
									    this.bounty
										    .get(render)
										    .getRewardItems()
										    .stream()
										    .map(RewardItem::getItem)
										    .map(
											    item -> "<lang:" + item.translationKey() + ">"
										    )
										    .toList()
								    )
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
						this.rdq.get(clickContext),
						"target",
						Optional.of(this.target.get(clickContext)),
						"rewardItems",
						this.bounty.get(clickContext).getRewardItems(),
						"rewardCurrencies",
						new HashMap<>(),
						"bounty",
						Optional.of(this.bounty.get(clickContext)),
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
					this.rdq.get(clickContext),
					"target",
					Optional.of(this.target.get(clickContext)),
					"rewardItems",
					this.bounty.get(clickContext).getRewardItems(),
					"rewardCurrencies",
					new HashMap<>(),
					"bounty",
					Optional.of(this.bounty.get(clickContext)),
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
				          clickContext.openForPlayer(
					          ConfirmationView.class,
					          Map.of(
						          "titleKey",
						          "confirm.delete.title",
						          "messageKey",
						          "confirm.delete.message",
						          "initialData",
						          clickContext.getInitialData()
					          )
				          )
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
		
		final RBounty bounty = (RBounty) initialData.get("bounty");
		if (
			bounty == null
		) {
			return;
		}
		
		final OfflinePlayer targetPlayer = (OfflinePlayer) initialData.get("target");
		
		final RDQImpl rdq = (RDQImpl) initialData.get("plugin");
		
		RDQPlayer rdqPlayer = bounty.getPlayer();
		rdqPlayer.setBounty(null);
		
		rdq.getPlayerRepository().updateAsync(
			rdqPlayer
		).thenAcceptAsync(
                player -> {
				rdq.getBountyRepository().deleteAsync(bounty.getId()).thenAccept(
					(v) -> {
						rdq.getBountyManager().removeBounty(
							targetPlayer.getUniqueId()
						);

                        I18n.create(
                                MessageKey.of("deleted_bounty_successfully"),
                                origin.getPlayer()
                        ).includePrefix().withPlaceholders(
                                Map.of(
                                        "bounty_id",
                                        bounty.getId(),
                                        "target_name",
                                        targetPlayer.getName(),
                                        "target_uniqueId",
                                        targetPlayer.getUniqueId()
                                )
                        ).sendMessage();
						
						rdq.getBountyRepository().delete(bounty.getId());
					}
				);
			},
			rdq.getExecutor()
		);
		
		target.openForPlayer(
			BountyMainView.class,
			Map.of(
				"plugin",
				rdq
			)
		);
	}
	
}