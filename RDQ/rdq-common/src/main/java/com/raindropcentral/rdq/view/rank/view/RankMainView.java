package com.raindropcentral.rdq.view.rank.view;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class RankMainView extends BaseView {

	private final State<RDQ> rdq = initialState("plugin");
	private       RDQPlayer  rdqPlayer;

	@Override
	protected String getKey() {
		return "rank_main_ui";
	}

	@Override
	protected String[] getLayout() {
		return new String[] { "         " };
	}

	@Override
	protected int getSize() {
		return 1;
	}

	@Override
	public void onFirstRender(
		final @NotNull RenderContext render,
		final @NotNull Player player
	) {
		this.rdq.get(render).getPlayerRepository().findByAttributesAsync(
			Map.of("uniqueId", player.getUniqueId())
		).thenAcceptAsync(
			rdqPlayer -> this.rdqPlayer = rdqPlayer,
			this.rdq.get(render).getExecutor()
		);

		render.slot(
			1, 5,
			UnifiedBuilderFactory.item(Material.CHAINMAIL_CHESTPLATE)
				.setName(this.i18n("rank_tree.name", player).build().component())
				.setLore(this.i18n("rank_tree.lore", player).build().splitLines())
				.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
				.build()
		).onClick(clickContext -> {
			clickContext.openForPlayer(
				RankTreeOverviewView.class,
				Map.of(
					"plugin", this.rdq.get(clickContext),
					"player", this.rdqPlayer
				)
			);
		});
	}
}