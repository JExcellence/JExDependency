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

/**
 * Main rank menu view providing navigation to rank management features.
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 1.0.0
 */
public final class RankMainView extends BaseView {

	private final State<RDQ> rdq = initialState("plugin");
	private       RDQPlayer  rdqPlayer;

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

		// Render decorations
		this.renderDecorations(render, player);

		// Render rank tree button
		render.layoutSlot(
			'r',
			UnifiedBuilderFactory.item(Material.CHAINMAIL_CHESTPLATE)
				.setName(this.i18n("rank_tree.name", player).build().component())
				.setLore(this.i18n("rank_tree.lore", player).build().splitLines())
				.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
				.setGlowing(true)
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

	/**
	 * Renders decorative glass pane borders for visual enhancement.
	 *
	 * @param render the render context used to populate slots
	 * @param player the player viewing the menu
	 */
	private void renderDecorations(
		final @NotNull RenderContext render,
		final @NotNull Player player
	) {
		render.layoutSlot(
			'G',
			UnifiedBuilderFactory
				.item(Material.PURPLE_STAINED_GLASS_PANE)
				.setName(this.i18n("decoration.name", player).build().component())
				.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
				.build()
		);
	}
}