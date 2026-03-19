package de.jexcellence.oneblock.view.framework;

import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.View;
import me.devnatan.inventoryframework.context.RenderContext;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Large Inventory View Framework
 * 
 * Provides a foundation for creating large (54-slot) inventory views with
 * standardized layouts, navigation patterns, and UI components.
 * 
 * Based on RDQ-style large inventory layouts with OneBlock-specific enhancements.
 * 
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public abstract class LargeInventoryView extends BaseView {
    
    protected static final int LARGE_INVENTORY_SIZE = 54; // 6 rows
    
    // Standard layout characters
    protected static final char BORDER_CHAR = 'X';
    protected static final char CONTENT_CHAR = 'O';
    protected static final char NAVIGATION_CHAR = 'N';
    protected static final char ACTION_CHAR = 'A';
    protected static final char INFO_CHAR = 'I';
    protected static final char CLOSE_CHAR = 'C';
    protected static final char BACK_CHAR = 'B';
    protected static final char HELP_CHAR = 'H';
    protected static final char REFRESH_CHAR = 'R';
    
    public LargeInventoryView() {
        super();
    }
    
    public LargeInventoryView(@Nullable Class<? extends View> parentClazz) {
        super(parentClazz);
    }
    
    // Layout templates for different view types
    public enum LayoutTemplate {
        FULL_CONTENT(new String[]{
            "XXXXXXXXX",
            "XOOOOOOOХ",
            "XOOOOOOOХ",
            "XOOOOOOOХ",
            "XOOOOOOOХ",
            " HACCCCRN"
        }),
        
        SIDEBAR_LEFT(new String[]{
            "XXXXXXXXX",
            "IIIOOOOOХ",
            "IIIOOOOOХ",
            "IIIOOOOOХ",
            "IIIOOOOOХ",
            " HACCCCRN"
        }),
        
        SIDEBAR_RIGHT(new String[]{
            "XXXXXXXXX",
            "XOOOOOIII",
            "XOOOOOIII",
            "XOOOOOIII",
            "XOOOOOIII",
            " HACCCCRN"
        }),
        
        DUAL_SIDEBAR(new String[]{
            "XXXXXXXXX",
            "IIOOOOOII",
            "IIOOOOOII",
            "IIOOOOOII",
            "IIOOOOOII",
            " HACCCCRN"
        }),
        
        HEADER_CONTENT(new String[]{
            "IIIIIIIII",
            "XOOOOOOOХ",
            "XOOOOOOOХ",
            "XOOOOOOOХ",
            "XOOOOOOOХ",
            " HACCCCRN"
        }),
        
        DASHBOARD(new String[]{
            "IIIIIIIII",
            "IIOOOOOII",
            "IIOOOOOII",
            "IIOOOOOII",
            "IIOOOOOII",
            " HACCCCRN"
        });
        
        private final String[] layout;
        
        LayoutTemplate(String[] layout) {
            this.layout = layout;
        }
        
        public String[] getLayout() {
            return layout.clone();
        }
    }
    
    /**
     * Gets the layout template for this view
     * 
     * @return the layout template to use
     */
    @NotNull
    protected abstract LayoutTemplate getLayoutTemplate();
    
    /**
     * Renders the main content area
     * 
     * @param render the render context
     * @param player the player viewing the UI
     */
    protected abstract void renderContent(@NotNull RenderContext render, @NotNull Player player);
    
    @Override
    protected final String[] getLayout() {
        return getLayoutTemplate().getLayout();
    }
    
    @Override
    public final void onFirstRender(@NotNull RenderContext render, @NotNull Player player) {
        // Render standard components
        renderBorder(render);
        renderNavigation(render, player);
        renderActionButtons(render, player);
        
        // Render view-specific content
        renderContent(render, player);
        
        // Render info panels if applicable
        if (hasInfoPanels()) {
            renderInfoPanels(render, player);
        }
    }
    
    /**
     * Renders the border elements
     */
    protected void renderBorder(@NotNull RenderContext render) {
        ItemStack borderItem = UnifiedBuilderFactory
            .item(Material.GRAY_STAINED_GLASS_PANE)
            .setName(Component.empty())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
        
        render.layoutSlot(BORDER_CHAR, borderItem);
    }
    
    /**
     * Renders navigation elements
     */
    protected void renderNavigation(@NotNull RenderContext render, @NotNull Player player) {
        // Close button
        ItemStack closeItem = UnifiedBuilderFactory
            .item(Material.BARRIER)
            .setName(i18n("navigation.close", player).build().component())
            .setLore(i18n("navigation.close.description", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
        
        render.layoutSlot(CLOSE_CHAR, closeItem)
            .onClick(click -> click.closeForPlayer());
        
        // Help button
        ItemStack helpItem = UnifiedBuilderFactory
            .item(Material.BOOK)
            .setName(i18n("navigation.help", player).build().component())
            .setLore(i18n("navigation.help.description", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
        
        render.layoutSlot(HELP_CHAR, helpItem)
            .onClick(click -> handleHelpClick(click.getPlayer()));
        
        // Refresh button
        ItemStack refreshItem = UnifiedBuilderFactory
            .item(Material.EMERALD)
            .setName(i18n("navigation.refresh", player).build().component())
            .setLore(i18n("navigation.refresh.description", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
        
        render.layoutSlot(REFRESH_CHAR, refreshItem)
            .onClick(click -> handleRefreshClick(click.getPlayer()));
    }
    
    /**
     * Renders action buttons
     */
    protected void renderActionButtons(@NotNull RenderContext render, @NotNull Player player) {
        List<ActionButton> actions = getActionButtons(player);
        
        if (!actions.isEmpty()) {
            render.layoutSlot(ACTION_CHAR, (slot, item) -> {
                int index = getLayoutCharIndex(ACTION_CHAR, slot);
                if (index >= 0 && index < actions.size()) {
                    ActionButton action = actions.get(index);
                    item.withItem(action.createItem(player))
                        .onClick(click -> action.onClick(click.getPlayer()));
                }
            });
        }
    }
    
    /**
     * Renders info panels (sidebar content)
     */
    protected void renderInfoPanels(@NotNull RenderContext render, @NotNull Player player) {
        List<InfoPanel> infoPanels = getInfoPanels(player);
        
        if (!infoPanels.isEmpty()) {
            render.layoutSlot(INFO_CHAR, (slot, item) -> {
                int index = getLayoutCharIndex(INFO_CHAR, slot);
                if (index >= 0 && index < infoPanels.size()) {
                    InfoPanel panel = infoPanels.get(index);
                    item.withItem(panel.createItem(player))
                        .onClick(click -> panel.onClick(click.getPlayer()));
                }
            });
        }
    }
    
    /**
     * Gets the index of a layout character occurrence for a specific slot.
     */
    private int getLayoutCharIndex(char layoutChar, int slot) {
        String[] layout = getLayout();
        int count = 0;
        
        for (int row = 0; row < layout.length; row++) {
            for (int col = 0; col < layout[row].length(); col++) {
                if (layout[row].charAt(col) == layoutChar) {
                    int currentSlot = row * 9 + col;
                    if (currentSlot == slot) {
                        return count;
                    }
                    count++;
                }
            }
        }
        
        return -1;
    }
    
    /**
     * Gets action buttons for this view
     * 
     * @param player the player viewing the UI
     * @return list of action buttons
     */
    @NotNull
    protected List<ActionButton> getActionButtons(@NotNull Player player) {
        return List.of();
    }
    
    /**
     * Gets info panels for this view
     * 
     * @param player the player viewing the UI
     * @return list of info panels
     */
    @NotNull
    protected List<InfoPanel> getInfoPanels(@NotNull Player player) {
        return List.of();
    }
    
    /**
     * Whether this view has info panels
     */
    protected boolean hasInfoPanels() {
        LayoutTemplate template = getLayoutTemplate();
        return template == LayoutTemplate.SIDEBAR_LEFT || 
               template == LayoutTemplate.SIDEBAR_RIGHT || 
               template == LayoutTemplate.DUAL_SIDEBAR ||
               template == LayoutTemplate.HEADER_CONTENT ||
               template == LayoutTemplate.DASHBOARD;
    }
    
    /**
     * Handles help button click
     */
    protected void handleHelpClick(@NotNull Player player) {
        i18n("help.default", player)
            .withPlaceholder("view", getKey())
            .build().sendMessage();
    }
    
    /**
     * Handles refresh button click
     */
    protected void handleRefreshClick(@NotNull Player player) {
        player.closeInventory();
        // Subclasses should override to reopen the view
    }
    
    /**
     * Action button definition
     */
    public static class ActionButton {
        private final Component name;
        private final List<Component> lore;
        private final Material material;
        private final java.util.function.Consumer<Player> clickHandler;
        
        public ActionButton(@NotNull Component name, @NotNull List<Component> lore, 
                           @NotNull Material material, @NotNull java.util.function.Consumer<Player> clickHandler) {
            this.name = name;
            this.lore = lore;
            this.material = material;
            this.clickHandler = clickHandler;
        }
        
        @NotNull
        public ItemStack createItem(@NotNull Player player) {
            return UnifiedBuilderFactory.item(material)
                .setName(name)
                .setLore(lore)
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
        }
        
        public void onClick(@NotNull Player player) {
            clickHandler.accept(player);
        }
    }
    
    /**
     * Info panel definition
     */
    public static class InfoPanel {
        private final Component name;
        private final List<Component> lore;
        private final Material material;
        private final java.util.function.Consumer<Player> clickHandler;
        
        public InfoPanel(@NotNull Component name, @NotNull List<Component> lore, 
                        @NotNull Material material, @NotNull java.util.function.Consumer<Player> clickHandler) {
            this.name = name;
            this.lore = lore;
            this.material = material;
            this.clickHandler = clickHandler;
        }
        
        public InfoPanel(@NotNull Component name, @NotNull List<Component> lore, @NotNull Material material) {
            this(name, lore, material, player -> {});
        }
        
        @NotNull
        public ItemStack createItem(@NotNull Player player) {
            return UnifiedBuilderFactory.item(material)
                .setName(name)
                .setLore(lore)
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
        }
        
        public void onClick(@NotNull Player player) {
            clickHandler.accept(player);
        }
    }
}