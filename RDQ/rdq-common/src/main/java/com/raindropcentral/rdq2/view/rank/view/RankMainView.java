/*
package com.raindropcentral.rdq2.view.rank.view;

import com.raindropcentral.rdq2.RDQ;
import com.raindropcentral.rdq2.database.entity.player.RDQPlayer;
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
	private RDQPlayer rdqPlayer;

	@Override
	protected String getKey() {
		return "rank_main_ui";
	}

	@Override
	protected String[] getLayout() {
		return new String[]{
			"GGGGGGGGG",
			"G       G",
			"G   r   G",
			"G       G",
			"GGGGGGGGG"
		};
	}

	@Override
	public void onFirstRender(final @NotNull RenderContext render, final @NotNull Player player) {
		rdq.get(render).getPlayerRepository()
			.findByAttributesAsync(Map.of("uniqueId", player.getUniqueId()))
			.thenAcceptAsync(
				rdqPlayer -> this.rdqPlayer = rdqPlayer,
				rdq.get(render).getExecutor()
			);

		renderDecorations(render, player);
		renderRankTreeButton(render, player);
	}

	private void renderDecorations(final @NotNull RenderContext render, final @NotNull Player player) {
		render.layoutSlot('G',
			UnifiedBuilderFactory.item(Material.PURPLE_STAINED_GLASS_PANE)
				.setName(i18n("decoration.name", player).build().component())
				.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
				.build()
		);
	}

	private void renderRankTreeButton(final @NotNull RenderContext render, final @NotNull Player player) {
		render.layoutSlot('r',
			UnifiedBuilderFactory.item(Material.CHAINMAIL_CHESTPLATE)
				.setName(i18n("rank_tree.name", player).build().component())
				.setLore(i18n("rank_tree.lore", player).build().splitLines())
				.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
				.setGlowing(true)
				.build()
		).onClick(clickContext -> {
			clickContext.openForPlayer(
				RankTreeOverviewView.class,
				Map.of(
					"plugin", rdq.get(clickContext),
					"player", rdqPlayer
				)
			);
		});
	}
}*/
