package com.raindropcentral.rdq.view.bounty;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.bounty.Bounty;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * View for displaying a paginated overview of all bounties in the system.
 * <p>
 * This view shows a list of all active bounties with player heads and basic information.
 * Players can click on individual bounties to view detailed information.
 * </p>
 */
public class BountyOverviewView extends APaginatedView<Bounty> {
	
	private final State<RDQ> rdq = initialState("plugin");
	
	public BountyOverviewView() {
		
		super(BountyMainView.class);
	}
	
	@Override
	protected String getKey() {
		
		return "bounty_overview_ui";
	}
	
	@Override
	protected CompletableFuture<List<Bounty>> getAsyncPaginationSource(
		final @NotNull Context context
	) {
		return rdq.get(context).getBountyRepository().findListByAttributesAsync(
			Map.of("active", true)
		);
	}
	
	@Override
	protected void renderEntry(
		final @NotNull Context context,
		final @NotNull BukkitItemComponentBuilder builder,
		final int index,
		final @NotNull Bounty bounty
	) {
		
		var target = Bukkit.getOfflinePlayer(bounty.getTargetUniqueId());
		var player        = context.getPlayer();

		builder
			.withItem(
				UnifiedBuilderFactory
					.unifiedHead(target)
					.setDisplayName(
						this.i18n(
							    "bounty.name",
							    player
						    )
						    .with(
							    "target_name",
									target.getName()
						    )
						    .build()
						    .component()
					)
					.setLore(
						this.i18n(
							    "bounty.lore",
							    player
						    )
						    .withAll(
							    Map.of(
								    "target_name",
										target.getName(),
								    "commissioner_name",
								    Bukkit.getOfflinePlayer(bounty.getCommissionerUniqueId()).getName(),
								    "created_at",
								    bounty.getCreatedAt().toLocalTime(),
								    "index",
								    index + 1
							    )
						    )
						    .build()
						    .splitLines()
					)
					.build()
			)
			.onClick(clickContext -> {
				clickContext.openForPlayer(
					BountyPlayerInfoView.class,
					Map.of(
						"plugin",
						this.rdq.get(clickContext),
						"bounty",
						Optional.of(bounty),
						"target",
						Optional.of(target),
						"parentClazz",
						BountyOverviewView.class,
						"initialData",
						clickContext.getInitialData()
					)
				);
			});
	}

	@Override
	protected @NotNull String[] getLayout() {
		return new String[]{
				"         ",
				" OOOOOOO ",
				"   <p>   ",
				"         "
		};
	}
	
	@Override
	protected void onPaginatedRender(
		final @NotNull RenderContext render,
		final @NotNull Player player
	) {
		// No additional rendering required for the bounty overview
		// All necessary elements are handled by the pagination system
	}
	
}