package com.raindropcentral.rdq.view.quests;

import com.raindropcentral.rdq.RDQImpl;
import com.raindropcentral.rplatform.view.common.BaseView;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * GUI view for displaying and managing player quests in RaindropQuests.
 * <p>
 * This view provides an interface for players to view and interact with their available quests.
 * It integrates with the InventoryFramework system and supports internationalized titles.
 * The view is initialized with a reference to the main plugin instance ({@link RDQImpl}).
 * </p>
 *
 * <ul>
 *   <li>Sets up the inventory size and title using i18n messages.</li>
 *   <li>Stores a reference to the main plugin instance for further operations.</li>
 *   <li>Intended to be extended with quest management buttons and logic.</li>
 * </ul>
 *
 * @author ItsRainingHP
 * @version 1.0.0
 * @since TBD
 */
public class QuestOverviewView extends BaseView {
    
    /**
     * State for storing the main plugin instance.
     * <p>
     * Used to retrieve the {@link RDQImpl} instance when the view is opened.
     * </p>
     */
    private final State<RDQImpl> rdq = initialState("plugin");
    
    @Override
    protected String getKey() {
        
        return "quest_overview_ui";
    }
    
    @Override
    public void onFirstRender(
        final @NotNull RenderContext render,
        final @NotNull Player player
    ) {
    
    }
    
}