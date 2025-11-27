package com.raindropcentral.rdq.view.bounty;

import com.raindropcentral.rdq.RDQImpl;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.common.BaseView;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

/**
 * Main entry point for the Bounty UI in RaindropQuests.
 * <p>
 * This view serves as the primary navigation menu for bounty-related actions, allowing players to:
 * <ul>
 *   <li>View all active bounties</li>
 *   <li>Create a new bounty</li>
 * </ul>
 * The view provides buttons for each action and manages navigation to the corresponding subviews.
 * </p>
 *
 * <p>
 * The view is initialized with a reference to the RDQImpl plugin instance and uses InventoryFramework
 * for GUI management. It leverages the organization's unified item builder and i18n systems for
 * consistent UI and localization.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class BountyMainView extends BaseView {
	
	/**
	 * The RDQImpl plugin instance, used for accessing repositories and services.
	 */
	private final State<RDQImpl> rdq = initialState("plugin");
	
	@Override
	protected String getKey() {
		
		return "bounty_overview_ui";
	}
	
	@Override
	public void onFirstRender(
		final @NotNull RenderContext render,
		final @NotNull Player player
	) {
		
		this.initializeCreateBountyButton(
			render,
			player
		);
		this.initializeOverviewButton(
			render,
			player
		);
	}
	
	/**
	 * Initializes the "View Bounties" button, which navigates to the bounty overview view.
	 *
	 * @param render the render context
	 */
	private void initializeOverviewButton(
		final @NotNull RenderContext render,
		final @NotNull Player player
	) {
		
		render
			.slot(
				10,
				UnifiedBuilderFactory
					.item(Material.DIAMOND_SWORD)
					.setName(
						this.i18n(
							"view_bounties.name",
							player
						).build().component()
					)
					.setLore(
						this.i18n(
							"view_bounties.lore",
							player
						).build().children()
					)
					.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
					.build()
			)
			.onClick(
				clickContext -> render.openForPlayer(
					BountyOverviewView.class,
					Map.of(
						"plugin",
						this.rdq.get(clickContext)
					)
				)
			);
	}
	
	/**
	 * Initializes the "Create Bounty" button, which navigates to the bounty creation view.
	 * Passes initial state for target, rewards, and currencies.
	 *
	 * @param render the render context
	 */
	private void initializeCreateBountyButton(
		final @NotNull RenderContext render,
		final @NotNull Player player
	) {
		
		render.slot(
			      11,
			      UnifiedBuilderFactory
				      .item(Material.EMERALD)
				      .setName(
					      this.i18n(
						      "create_bounty.name",
						      player
					      ).build().component()
				      )
				      .setLore(
					      this.i18n(
						      "create_bounty.lore",
						      player
					      ).build().children()
				      )
				      .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
				      .build()
		      )
		      .onClick(clickContext -> clickContext.openForPlayer(
			      BountyCreationView.class,
			      Map.of(
				      "plugin",
				      this.rdq.get(render),
				      "target",
				      Optional.empty(),
				      "rewardItems",
				      new HashSet<>(),
				      "rewardCurrencies",
				      new HashMap<>(),
				      "bounty",
				      Optional.empty(),
				      "insertedItems",
				      new HashMap<>()
			      )
		      ));
	}
	
}
