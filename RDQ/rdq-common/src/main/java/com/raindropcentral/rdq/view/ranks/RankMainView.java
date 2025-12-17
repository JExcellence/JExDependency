package com.raindropcentral.rdq.view.ranks;


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
 * GUI view for displaying and managing player ranks in RaindropQuests.
 * <p>
 * This view provides an interface for players to view and interact with their available ranks.
 * It integrates with the InventoryFramework system and supports internationalized titles.
 * The view is initialized with a reference to the main plugin instance ({@link RDQ}).
 * </p>
 *
 * <ul>
 *   <li>Sets up the inventory size and title using i18n messages.</li>
 *   <li>Stores a reference to the main plugin instance for further operations.</li>
 *   <li>Intended to be extended with rank management buttons and logic.</li>
 * </ul>
 *
 * @author ItsRainingHP
 * @version 1.0.0
 * @since TBD
 */
public class RankMainView extends BaseView {
    
    /**
     * State holding the reference to the main RDQ plugin instance.
     * Used for accessing plugin services and repositories within the view.
     */
    private final State<RDQ> rdq = initialState("plugin");
    
    private RDQPlayer rdqPlayer;
    
    @Override
    protected String getKey() {
        
        return "rank_main_ui";
    }
    
    /**
     * Returns the layout of the inventory as rows.
     * Each string represents one row of 9 slots.
     *
     * @return An array of strings representing the inventory layout.
     */
    @Override
    protected String[] getLayout() {
        return new String[] {
            "         "
        };
    }
    
    /**
     * Returns the size (number of rows) of the inventory.
     *
     * @return The number of rows for the inventory (1).
     */
    @Override
    protected int getSize() {
        return 1;
    }
    
    @Override
    public void onFirstRender(
        final @NotNull RenderContext render,
        final @NotNull Player player
    ) {
        // Load player synchronously to ensure it's available when clicking
        // This is a quick database lookup so it's acceptable
        this.rdqPlayer = this.rdq.get(render).getPlayerRepository().findByAttributes(
            Map.of("uniqueId", player.getUniqueId())
        ).orElse(null);
        
        render
            .slot(
                1,
                5,
                UnifiedBuilderFactory
                    .item(Material.CHAINMAIL_CHESTPLATE)
                    .setName(
                            this.i18n("rank_tree.name", player).build().component()
                    )
                    .setLore(
                            this.i18n("rank_tree.lore", player).build().children()
                    )
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .build()
            )
            .onClick(
                clickContext -> {
                    if (this.rdqPlayer == null) {
                        // Try to load again if still null
                        this.rdqPlayer = this.rdq.get(clickContext).getPlayerRepository().findByAttributes(
                            Map.of("uniqueId", clickContext.getPlayer().getUniqueId())
                        ).orElse(null);
                    }
                    
                    if (this.rdqPlayer != null) {
                        clickContext.openForPlayer(
                            RankTreeOverviewView.class,
                            Map.of(
                                "plugin",
                                this.rdq.get(clickContext),
                                "player",
                                this.rdqPlayer
                            )
                        );
                    }
                }
            );
    }
    
}