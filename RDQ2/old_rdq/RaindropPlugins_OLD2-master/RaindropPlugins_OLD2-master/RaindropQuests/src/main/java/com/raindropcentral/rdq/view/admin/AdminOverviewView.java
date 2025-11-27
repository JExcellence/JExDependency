package com.raindropcentral.rdq.view.admin;

import com.raindropcentral.rdq.RDQImpl;
import com.raindropcentral.rplatform.api.luckperms.IRank;
import com.raindropcentral.rplatform.logger.CentralLogger;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.common.BaseView;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

/**
 * Administrative overview GUI for RaindropQuests.
 * <p>
 * This view serves as the main entry point for administrators, providing access to various
 * administrative actions and sub-views, such as permissions management. It leverages
 * InventoryFramework for GUI management and R18n for internationalized messages.
 * </p>
 *
 * <ul>
 *     <li>Displays a button to access the permissions management view.</li>
 *     <li>Provides group creation functionality with proper error handling.</li>
 *     <li>Uses the {@link RDQImpl} plugin instance for context and service access.</li>
 *     <li>Supports internationalized titles and item names/lore with proper feedback.</li>
 * </ul>
 *
 * @author ItsRainingHP
 * @version 2.0.0
 * @since TBD
 */
public class AdminOverviewView extends BaseView {
	
	/**
	 * State for storing the main plugin instance.
	 */
	private final State<RDQImpl> rdq = initialState("plugin");
	
	@Override
	protected String getKey() {
		return "admin_overview_ui";
	}
	
	@Override
	protected int getSize() {
		return 3;
	}
	
	@Override
	public void onFirstRender(
		final @NotNull RenderContext render,
		final @NotNull Player player
	) {
		this.initializePermissionsViewButton(render, player);
		this.initializeGroupCreationButton(render, player);
	}
	
	/**
	 * Initializes the button that opens the permissions management view.
	 * <p>
	 * The button is displayed as a diamond item with internationalized name and lore.
	 * When clicked, it opens the {@link AdminPermissionsView} for the player, passing the plugin instance as state.
	 * </p>
	 *
	 * @param context The render context for the current inventory.
	 * @param player  The player viewing the GUI.
	 */
	private void initializePermissionsViewButton(
		final @NotNull RenderContext context,
		final @NotNull Player player
	) {
		Map<String, List<String>> permissionsMap = this.rdq.get(context).getPermissionsService().getPermissions();
		int totalPlugins = permissionsMap.size();
		int totalPermissions = permissionsMap.values().stream().mapToInt(List::size).sum();
		
		context.slot(1, 1)
		       .withItem(
			       UnifiedBuilderFactory.item(Material.DIAMOND)
			                            .setName(this.i18n("view_permissions.name", player).build().component())
			                            .setLore(this.i18n("view_permissions.lore", player)
			                                         .withPlaceholders(Map.of(
				                                         "total_plugins", totalPlugins,
				                                         "total_permissions", totalPermissions
			                                         )).build().children())
			                            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
			                            .build()
		       ).onClick(clickContext -> {
			       try {
				       clickContext.openForPlayer(
					       AdminPermissionsView.class,
					       Map.of("plugin", this.rdq.get(clickContext))
				       );
			       } catch (
					   final Exception exception
			       ) {
				       CentralLogger.getLogger(AdminOverviewView.class).log(
					       Level.SEVERE,
					       "Failed to open permissions view for player: " + player.getName(),
					       exception
				       );
				       
				       this.i18n("view_permissions.error", player)
				           .includePrefix()
				           .sendMessage();
			       }
		       });
	}
	
	/**
	 * Initializes the group creation button with enhanced feedback and error handling.
	 */
	private void initializeGroupCreationButton(
		final @NotNull RenderContext context,
		final @NotNull Player player
	) {
		context.slot(1, 2)
		       .withItem(
			       UnifiedBuilderFactory.item(Material.REDSTONE)
			                            .setName(this.i18n("create_ranks.name", player).build().component())
			                            .setLore(this.i18n("create_ranks.lore", player).build().children())
			                            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
			                            .build()
		       ).onClick(clickContext -> {
			       try {
				       // Check if LuckPerms service is available
				       if (this.rdq.get(clickContext).getLuckPermsService() == null) {
					       this.i18n("create_ranks.no_luckperms", player)
					           .includePrefix()
					           .sendMessage();
					       return;
				       }
				       
				       Map<String, List<IRank>> groups = new HashMap<>();
				       
				       this.rdq.get(clickContext).getRankSystemFactory().getRanks().forEach((key, value) ->
					                                                                            value.forEach((rankPathIdentifier, rank) -> {
						                                                                            if (!groups.containsKey(rank.getAssignedLuckPermsGroup())) {
							                                                                            groups.put(rank.getAssignedLuckPermsGroup(), new ArrayList<>());
						                                                                            }
						                                                                            
						                                                                            groups.get(rank.getAssignedLuckPermsGroup()).add(
							                                                                            new IRRank(
								                                                                            rank.getIdentifier(),
								                                                                            rank.getDisplayNameKey(),
								                                                                            rank.getWeight()
							                                                                            )
						                                                                            );
					                                                                            })
				       );
				       
				       if (groups.isEmpty()) {
					       this.i18n("create_ranks.no_ranks_found", player)
					           .includePrefix()
					           .sendMessage();
					       return;
				       }
				       
				       int created = this.rdq.get(clickContext).getLuckPermsService().createOrUpdateGroups(groups);
				       
				       if (created > 0) {
					       this.i18n("create_ranks.created", player)
					           .withPlaceholder("groups_created", created)
					           .includePrefix()
					           .sendMessage();
				       } else {
					       this.i18n("create_ranks.no_changes", player)
					           .includePrefix()
					           .sendMessage();
				       }
				       
			       } catch (ExecutionException | InterruptedException exception) {
				       CentralLogger.getLogger(AdminOverviewView.class).log(
					       Level.SEVERE,
					       "Failed to create/update groups for player: " + player.getName(),
					       exception
				       );
				       
				       this.i18n("create_ranks.error", player)
				           .includePrefix()
				           .sendMessage();
			       }
		       });
	}
}