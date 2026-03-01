package com.raindropcentral.rdq.view.main;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.view.admin.AdminOverviewView;
import com.raindropcentral.rdq.view.bounty.BountyOverviewView;
import com.raindropcentral.rdq.view.ranks.RankMainView;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Main overview GUI for RaindropQuests.
 * <p>
 * This view serves as the primary menu for players, providing navigation to all major subsystems
 * of the plugin, including bounties, perks, quests, ranks, and (for authorized users) admin controls.
 * It leverages InventoryFramework for GUI management and R18n for internationalized messages.
 * </p>
 *
 * <ul>
 *     <li>Displays navigation buttons for Bounties, Perks, Quests, and Ranks.</li>
 *     <li>Displays an Admin button for users with the appropriate permission or operator status.</li>
 *     <li>Uses the {@link com.raindropcentral.rdq.RDQ} plugin instance for context and service access.</li>
 *     <li>Supports internationalized titles and item names/lore.</li>
 * </ul>
 *
 * @author ItsRainingHP
 * @version 1.0.0
 * @since TBD
 */
public class MainOverviewView extends BaseView {
    
    /**
     * State for storing the main plugin instance.
     */
    private final State<RDQ> rdq = initialState("plugin");
    
    @Override
    protected String getKey() {
        
        return "main_overview_ui";
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
        
        this.initializeAdminViewButton(
            render,
            player
        );
        
        this.initializeBountyViewButton(
            render,
            player
        );
        this.initializePerkViewButton(
            render,
            player
        );
        this.initializeQuestViewButton(
            render,
            player
        );
        this.initializeRankViewButton(
            render,
            player
        );
    }
    
    /**
     * Initializes the Admin Overview button.
     * <p>
     * Only visible to players with the "raindropquests.command.admin" permission or operator status.
     * Opens the {@link AdminOverviewView} when clicked.
     * </p>
     *
     * @param context The render context for the current inventory.
     * @param player  The player viewing the GUI.
     */
    private void initializeAdminViewButton(
        final @NotNull RenderContext context,
        final @NotNull Player player
    ) {
        
        context
            .slot(
                1,
                9
            )
            .withItem(
                UnifiedBuilderFactory
                    .item(
                        Material.DIAMOND
                    )
                    .setName(
                        this.i18n(
                                "admin_overview.name",
                                player
                            )
                            .build()
                            .component()
                    )
                    .setLore(
                        this.i18n(
                                "admin_overview.lore",
                                player
                            )
                            .build()
                            .children()
                    )
                    .build()
            )
            .displayIf(() ->
                player.hasPermission("raindropquests.command.admin") ||
                player.hasPermission("raindropquests.command") ||
                player.isOp()
            )
            .onClick(clickContext -> {
                clickContext.openForPlayer(
                    AdminOverviewView.class,
                    Map.of(
                        "plugin",
                        this.rdq.get(clickContext)
                    )
                );
            })
        ;
    }

    private void initializeBountyViewButton(
        final @NotNull RenderContext context,
        final @NotNull Player player
    ) {
        
        context
            .slot(
                1,
                1
            )
            .withItem(
                UnifiedBuilderFactory
                    .item(
                        Material.DIAMOND
                    )
                    .setName(
                        this.i18n(
                                "bounty_overview.name",
                                player
                            )
                            .build()
                            .component()
                    )
                    .setLore(
                        this.i18n(
                                "bounty_overview.lore",
                                player
                            )
                            .build()
                            .children()
                    )
                    .build()
            )
            .onClick(clickContext -> {
                clickContext.openForPlayer(
                    BountyOverviewView.class,
                    Map.of(
                        "plugin",
                            this.rdq.get(clickContext)
                    )
                );
            })
        ;
    }

    private void initializePerkViewButton(
        final @NotNull RenderContext context,
        final @NotNull Player player
    ) {
        
        context
            .slot(
                1,
                2
            )
            .withItem(
                UnifiedBuilderFactory
                    .item(
                        Material.DIAMOND
                    )
                    .setName(
                        this.i18n(
                                "perk_overview.name",
                                player
                            )
                            .build()
                            .component()
                    )
                    .setLore(
                        this.i18n(
                                "perk_overview.lore",
                                player
                            )
                            .build()
                            .children()
                    )
                    .build()
            )
            .onClick(clickContext -> {

            })
        ;
    }

    private void initializeQuestViewButton(
        final @NotNull RenderContext context,
        final @NotNull Player player
    ) {
        
        context
            .slot(
                1,
                3
            )
            .withItem(
                UnifiedBuilderFactory
                    .item(
                        Material.DIAMOND
                    )
                    .setName(
                        this.i18n(
                                "quest_overview.name",
                                player
                            )
                            .build()
                            .component()
                    )
                    .setLore(
                        this.i18n(
                                "quest_overview.lore",
                                player
                            )
                            .build()
                            .children()
                    )
                    .build()
            )
            .onClick(clickContext -> {

            })
        ;
    }
    
    /**
     * Initializes the button for opening the Ranks Overview view in the main overview GUI.
     * <p>
     * Sets the button's item, name, and lore using internationalized messages,
     * </p>
     *
     * @param context The render context containing inventory and view information.
     * @param player  The player for whom the button is being initialized.
     */
    private void initializeRankViewButton(
        final @NotNull RenderContext context,
        final @NotNull Player player
    ) {
        
        context
            .slot(
                1,
                4
            )
            .withItem(
                UnifiedBuilderFactory
                    .item(
                        Material.DIAMOND
                    )
                    .setName(
                        this.i18n(
                                "rank_overview.name",
                                player
                            )
                            .build()
                            .component()
                    )
                    .setLore(
                        this.i18n(
                                "rank_overview.lore",
                                player
                            )
                            .build()
                            .children()
                    )
                    .build()
            )
            .onClick(clickContext -> {
                clickContext.openForPlayer(
                    RankMainView.class,
                    Map.of(
                        "plugin",
                        this.rdq.get(clickContext)
                    )
                );
            })
        ;
    }
    
}
