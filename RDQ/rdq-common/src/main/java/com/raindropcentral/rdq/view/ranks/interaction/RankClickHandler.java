package com.raindropcentral.rdq.view.ranks.interaction;


import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.type.ERankStatus;
import com.raindropcentral.rdq.view.ranks.hierarchy.RankNode;
import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.context.SlotClickContext;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles rank click interactions and delegates to appropriate handlers.
 *
 * This class serves as the main entry point for rank interactions,
 * routing different types of clicks to the appropriate handlers.
 */
public class RankClickHandler {
	
	private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
	
	private final RankProgressionManager progressionManager;
	
	/**
	 * Executes RankClickHandler.
	 */
	public RankClickHandler(final @NotNull RankProgressionManager progressionManager) {
		this.progressionManager = progressionManager;
	}
	
	/**
	 * Handles a rank click based on the rank status and click type.
	 */
	public void handleRankClick(
		final @NotNull SlotClickContext clickContext,
		final @NotNull RankNode rankNode,
		final @NotNull ERankStatus rankStatus,
		final @NotNull RDQPlayer rdqPlayer,
		final boolean previewMode
	) {
		try {
			final Player player = clickContext.getPlayer();
			final ClickType clickType = clickContext.getClickOrigin().getClick();
			
			LOGGER.log(Level.FINE, "Handling rank click: " + rankNode.rank.getIdentifier() + " - Status: " + rankStatus + " - Preview: " + previewMode + " - Click: " + clickType);
			
			if (
				previewMode
			) {
				this.handlePreviewModeClick(player, rankNode);
				return;
			}
			
			switch (rankStatus) {
				case OWNED -> this.handleOwnedRankClick(player, rankNode);
				case AVAILABLE -> this.handleAvailableRankClick(clickContext, rankNode, clickType, rdqPlayer);
				case IN_PROGRESS -> this.handleInProgressRankClick(clickContext, rankNode, clickType);
				case LOCKED -> this.handleLockedRankClick(player, rankNode);
			}
			
		} catch (final Exception exception) {
			LOGGER.log(Level.SEVERE, "Failed to handle rank click", exception);
			this.sendErrorMessage(clickContext.getPlayer(), "rank_click.error.general");
		}
	}
	
	/**
	 * Handles clicks in preview mode.
	 */
	private void handlePreviewModeClick(final @NotNull Player player, final @NotNull RankNode rankNode) {
		try {
            new I18n.Builder("rank_click.preview_mode", player)
				.withPlaceholder("rank_name", rankNode.rank.getIdentifier())
				.includePrefix()
				.build().sendMessage();
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to send preview mode message", exception);
		}
	}
	
	/**
	 * Handles clicks on owned ranks.
	 */
	private void handleOwnedRankClick(final @NotNull Player player, final @NotNull RankNode rankNode) {
		try {
            new I18n.Builder("rank_click.owned", player)
				.withPlaceholder("rank_name", rankNode.rank.getIdentifier())
				.includePrefix()
				.build().sendMessage();
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to send owned rank message", exception);
		}
	}
	
	/**
	 * Handles clicks on available ranks.
	 */
	private void handleAvailableRankClick(
		final @NotNull SlotClickContext clickContext,
		final @NotNull RankNode rankNode,
		final @NotNull ClickType clickType,
		final @NotNull RDQPlayer rdqPlayer
		) {
		if (
			clickType == ClickType.LEFT
		) {
			this.progressionManager.startRankProgression(clickContext, rankNode, rdqPlayer);
		} else if (
			clickType == ClickType.RIGHT
		) {
			this.progressionManager.openRequirementsView(clickContext, rankNode);
		}
	}
	
	/**
	 * Handles clicks on in-progress ranks.
	 */
	private void handleInProgressRankClick(
		final @NotNull SlotClickContext clickContext,
		final @NotNull RankNode rankNode,
		final @NotNull ClickType clickType
	) {
		if (clickType == ClickType.LEFT) {
			this.progressionManager.attemptRankRedemption(clickContext, rankNode);
		} else if (clickType == ClickType.RIGHT) {
			this.progressionManager.openRequirementsView(clickContext, rankNode);
		}
	}
	
	/**
	 * Handles clicks on locked ranks.
	 */
	private void handleLockedRankClick(final @NotNull Player player, final @NotNull RankNode rankNode) {
		try {
            new I18n.Builder("rank_click.locked", player)
				.withPlaceholder("rank_name", rankNode.rank.getIdentifier())
				.includePrefix()
				.build().sendMessage();
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to send locked rank message", exception);
		}
	}
	
	/**
	 * Sends an error message to the player.
	 */
	private void sendErrorMessage(final @NotNull Player player, final @NotNull String messageKey) {
		try {
            new I18n.Builder(messageKey, player)
				.includePrefix()
				.build().sendMessage();
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to send error message", exception);
		}
	}
}
