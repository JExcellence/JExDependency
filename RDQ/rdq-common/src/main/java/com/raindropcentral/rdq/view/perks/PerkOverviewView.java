package com.raindropcentral.rdq.view.perks;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.perk.Perk;
import com.raindropcentral.rdq.database.entity.perk.PlayerPerk;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.perk.PerkManagementService;
import com.raindropcentral.rdq.view.perks.util.PerkCardRenderer;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.view.APaginatedView;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Paginated GUI view for displaying all perks.
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class PerkOverviewView extends APaginatedView<Perk> {

    private static final Logger LOGGER = CentralLogger.getLogger("RDQ");

    // State
    private final State<RDQ> rdq = initialState("plugin");
    private final State<RDQPlayer> currentPlayer = initialState("player");

    private PerkCardRenderer cardRenderer;

    public PerkOverviewView() {
        super();
    }

    @Override
    protected String getKey() {
        return "perk_overview_ui";
    }

    @Override
    protected CompletableFuture<List<Perk>> getAsyncPaginationSource(final @NotNull Context context) {
        return CompletableFuture.supplyAsync(() -> {
            final RDQ plugin = rdq.get(context);
            final PerkManagementService managementService = plugin.getPerkManagementService();
            
            // Pass null to get all perks regardless of category
            return managementService.getAvailablePerks(null).stream()
                    .sorted(Comparator.comparingInt(Perk::getDisplayOrder))
                    .collect(Collectors.toList());
        });
    }

    @Override
    protected void renderEntry(
            final @NotNull Context context,
            final @NotNull BukkitItemComponentBuilder builder,
            final int index,
            final @NotNull Perk perk
    ) {
        final RDQ plugin = rdq.get(context);
        final RDQPlayer rdqPlayer = currentPlayer.get(context);
        final Player player = context.getPlayer();
        
        if (rdqPlayer == null) {
            return;
        }

        // Initialize card renderer if needed
        if (cardRenderer == null) {
            cardRenderer = new PerkCardRenderer(plugin.getPerkRequirementService());
        }

        final PerkManagementService managementService = plugin.getPerkManagementService();
        final Optional<PlayerPerk> playerPerkOpt = managementService.getPlayerPerk(rdqPlayer, perk);

        builder.withItem(cardRenderer.renderPerkCard(player, perk, playerPerkOpt.orElse(null)))
                .onClick(clickContext -> {
                    clickContext.getPlayer().playSound(
                            clickContext.getPlayer().getLocation(),
                            Sound.UI_BUTTON_CLICK,
                            1.0f,
                            1.0f
                    );
                    clickContext.openForPlayer(PerkDetailView.class, Map.of(
                            "plugin", plugin,
                            "player", rdqPlayer,
                            "perk", perk
                    ));
                });
    }

    @Override
    protected void onPaginatedRender(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        // Additional rendering can be done here if needed
        // For now, pagination handles everything
    }
}
