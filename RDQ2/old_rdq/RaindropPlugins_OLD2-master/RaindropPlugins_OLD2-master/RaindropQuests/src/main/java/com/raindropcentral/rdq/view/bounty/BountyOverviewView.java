package com.raindropcentral.rdq.view.bounty;

import com.raindropcentral.rdq.RDQImpl;
import com.raindropcentral.rdq.database.entity.RBounty;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.common.APaginatedView;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * View for displaying a paginated overview of all bounties in the system.
 * <p>
 * This view shows a list of all active bounties with player heads and basic information.
 * Players can click on individual bounties to view detailed information.
 * </p>
 */
public class BountyOverviewView extends APaginatedView<RBounty> {
	
	private final State<RDQImpl> rdq = initialState("plugin");
	
	public BountyOverviewView() {
		
		super(BountyMainView.class);
	}
	
	@Override
	protected String getKey() {
		
		return "bounty_overview_ui";
	}
	
	@Override
	protected CompletableFuture<List<RBounty>> getAsyncPaginationSource(
		final @NotNull Context context
	) {
		
		return rdq.get(context).getBountyRepository().findAllAsync(
			1,
			128
		);
	}
	
	@Override
	protected void renderEntry(
		final @NotNull Context context,
		final @NotNull BukkitItemComponentBuilder builder,
		final int index,
		final @NotNull RBounty bounty
	) {
		
		final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(bounty.getPlayer().getUniqueId());
		final Player        player        = context.getPlayer();
		
		builder
			.withItem(
				UnifiedBuilderFactory
					.head()
					.setPlayerHead(offlinePlayer)
					.setName(
						this.i18n(
							    "bounty.name",
							    player
						    )
						    .withPlaceholder(
							    "target_name",
							    bounty.getPlayer().getPlayerName()
						    )
						    .build()
						    .component()
					)
					.setLore(
						this.i18n(
							    "bounty.lore",
							    player
						    )
						    .withPlaceholders(
							    Map.of(
								    "target_name",
								    bounty.getPlayer().getPlayerName(),
								    "commissioner_name",
								    Bukkit.getOfflinePlayer(bounty.getCommissioner()).getName(),
								    "created_at",
								    bounty.getCreatedAt().toLocalTime(),
								    "index",
								    index + 1
							    )
						    )
						    .build()
						    .children()
					)
					.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
					.build()
			)
			.onClick(clickContext -> {
				clickContext.openForPlayer(
					BountyPlayerInfoView.class,
					Map.of(
						"plugin",
						this.rdq.get(clickContext),
						"bounty",
						bounty,
						"target",
						offlinePlayer,
						"parentClazz",
						BountyOverviewView.class,
						"initialData",
						clickContext.getInitialData()
					)
				);
			});
	}
	
	@Override
	protected void onPaginatedRender(
		final @NotNull RenderContext render,
		final @NotNull Player player
	) {
	
	}
	
}