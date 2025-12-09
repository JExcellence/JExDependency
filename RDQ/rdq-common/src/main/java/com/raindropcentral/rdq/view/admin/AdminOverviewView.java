package com.raindropcentral.rdq.view.admin;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rplatform.api.luckperms.IRank;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
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

public class AdminOverviewView extends BaseView {
	
	/**
	 * State for storing the main plugin instance.
	 */
	private final State<RDQ> rdq = initialState("plugin");
	
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
		Map<String, List<String>> permissionsMap = rdq.get(context).getPermissionsService().getPermissions();
		int totalPlugins = permissionsMap.size();
		int totalPermissions = permissionsMap.values().stream().mapToInt(List::size).sum();
		
		context.slot(1, 1)
		       .withItem(
			       UnifiedBuilderFactory.item(Material.DIAMOND)
			                            .setName(i18n("view_permissions.name", player).build().component())
			                            .setLore(i18n("view_permissions.lore", player)
			                                         .withAll(Map.of(
				                                         "total_plugins", totalPlugins,
				                                         "total_permissions", totalPermissions
			                                         )).build().splitLines())
			                            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
			                            .build()
		       ).onClick(clickContext -> {
			       try {
				       clickContext.openForPlayer(
					       AdminPermissionsView.class,
					       Map.of("plugin", rdq.get(clickContext))
				       );
			       } catch (
					   final Exception exception
			       ) {
				       CentralLogger.getLogger(AdminOverviewView.class).log(
					       Level.SEVERE,
					       "Failed to open permissions view for player: " + player.getName(),
					       exception
				       );
				       
				       i18n("view_permissions.error", player).withPrefix().send();
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
			                            .setName(i18n("create_ranks.name", player).build().component())
			                            .setLore(i18n("create_ranks.lore", player).build().splitLines())
			                            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
			                            .build()
		       ).onClick(clickContext -> {
			       try {
				       // Check if LuckPerms service is available
				       if (rdq.get(clickContext).getLuckPermsService() == null) {
					       i18n("create_ranks.no_luckperms", player)
					           .withPrefix()
					           .send();
					       return;
				       }
				       
				       Map<String, List<IRank>> groups = new HashMap<>();
				       
				       rdq.get(clickContext).getRankSystemFactory().getRanks().forEach((key, value) ->
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
					       i18n("create_ranks.no_ranks_found", player)
					           .withPrefix()
					           .send();
					       return;
				       }
				       
				       int created = rdq.get(clickContext).getLuckPermsService().createOrUpdateGroups(groups);
				       
				       if (created > 0) {
					       i18n("create_ranks.created", player)
					           .with("groups_created", created)
					           .withPrefix()
					           .send();
				       } else {
					       i18n("create_ranks.no_changes", player)
					           .withPrefix()
					           .send();
				       }
				       
			       } catch (ExecutionException | InterruptedException exception) {
				       CentralLogger.getLogger(AdminOverviewView.class).log(
					       Level.SEVERE,
					       "Failed to create/update groups for player: " + player.getName(),
					       exception
				       );
				       
				       i18n("create_ranks.error", player)
				           .withPrefix()
				           .send();
			       }
		       });
	}
}