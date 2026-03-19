package de.jexcellence.oneblock.view.framework;

import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import me.devnatan.inventoryframework.View;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.MutableState;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Paginated Large View Framework
 * 
 * Extends LargeInventoryView to provide manual pagination functionality for large datasets.
 * Uses simple page tracking with mutable state.
 * 
 * @param <T> the type of elements to paginate
 * 
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public abstract class PaginatedLargeView<T> extends LargeInventoryView {
    
    // Pagination layout characters
    protected static final char PAGINATION_CHAR = 'O';
    protected static final char PREVIOUS_CHAR = '<';
    protected static final char NEXT_CHAR = '>';
    protected static final char PAGE_INFO_CHAR = 'P';
    
    // Current page state
    private final MutableState<Integer> currentPage = mutableState(0);
    
    public PaginatedLargeView() {
        super();
    }
    
    public PaginatedLargeView(@Nullable Class<? extends View> parentClazz) {
        super(parentClazz);
    }
    
    /**
     * Enhanced layout templates with pagination support
     */
    public enum PaginatedLayoutTemplate {
        FULL_PAGINATED(new String[]{
            "XXXXXXXXX",
            "XOOOOOOOХ",
            "XOOOOOOOХ",
            "XOOOOOOOХ",
            "XOOOOOOOХ",
            " H<P>ACRN"
        }),
        
        SIDEBAR_PAGINATED(new String[]{
            "XXXXXXXXX",
            "IIIOOOOOХ",
            "IIIOOOOOХ",
            "IIIOOOOOХ",
            "IIIOOOOOХ",
            " H<P>ACRN"
        }),
        
        DASHBOARD_PAGINATED(new String[]{
            "IIIIIIIII",
            "IIOOOOOII",
            "IIOOOOOII",
            "IIOOOOOII",
            "IIOOOOOII",
            " H<P>ACRN"
        }),
        
        GRID_PAGINATED(new String[]{
            "XXXXXXXXX",
            "XOOOOOOOХ",
            "XOOOOOOOХ",
            "XOOOOOOOХ",
            "XOOOOOOOХ",
            "X<PPPPP>X"
        });
        
        private final String[] layout;
        
        PaginatedLayoutTemplate(String[] layout) {
            this.layout = layout;
        }
        
        public String[] getLayout() {
            return layout.clone();
        }
    }
    
    /**
     * Gets the paginated layout template for this view
     */
    @NotNull
    protected abstract PaginatedLayoutTemplate getPaginatedLayoutTemplate();
    
    /**
     * Provides the data source for pagination
     */
    @NotNull
    protected abstract List<T> getPaginationSource(@NotNull RenderContext render, @NotNull Player player);
    
    /**
     * Renders a single paginated entry
     */
    @NotNull
    protected abstract ItemStack renderPaginatedItem(@NotNull RenderContext render, @NotNull Player player, 
                                                     @NotNull T item, int index);
    
    @Override
    @NotNull
    protected final LayoutTemplate getLayoutTemplate() {
        PaginatedLayoutTemplate paginatedTemplate = getPaginatedLayoutTemplate();
        return switch (paginatedTemplate) {
            case FULL_PAGINATED -> LayoutTemplate.FULL_CONTENT;
            case SIDEBAR_PAGINATED -> LayoutTemplate.SIDEBAR_LEFT;
            case DASHBOARD_PAGINATED -> LayoutTemplate.DASHBOARD;
            case GRID_PAGINATED -> LayoutTemplate.FULL_CONTENT;
        };
    }
    
    @Override
    protected final void renderContent(@NotNull RenderContext render, @NotNull Player player) {
        List<T> items = getPaginationSource(render, player);
        renderPaginationContent(render, player, items);
        renderPaginationNavigation(render, player, items);
        renderAdditionalContent(render, player);
    }
    
    protected void renderPaginationContent(@NotNull RenderContext render, @NotNull Player player, @NotNull List<T> items) {
        int page = currentPage.get(render);
        int itemsPerPage = getItemsPerPage();
        int startIndex = page * itemsPerPage;
        List<Integer> paginationSlots = getPaginationSlots();
        
        for (int i = 0; i < paginationSlots.size(); i++) {
            int itemIndex = startIndex + i;
            int slot = paginationSlots.get(i);
            
            if (itemIndex < items.size()) {
                T item = items.get(itemIndex);
                render.slot(slot)
                    .renderWith(() -> renderPaginatedItem(render, player, item, itemIndex))
                    .onClick(click -> handleItemClick(click.getPlayer(), item, itemIndex))
                    .updateOnStateChange(currentPage);
            } else {
                render.slot(slot)
                    .renderWith(() -> createEmptySlot(player))
                    .updateOnStateChange(currentPage);
            }
        }
    }
    
    @NotNull
    protected List<Integer> getPaginationSlots() {
        String[] layout = getPaginatedLayoutTemplate().getLayout();
        List<Integer> slots = new ArrayList<>();
        
        for (int row = 0; row < layout.length; row++) {
            for (int col = 0; col < layout[row].length(); col++) {
                if (layout[row].charAt(col) == PAGINATION_CHAR) {
                    slots.add(row * 9 + col);
                }
            }
        }
        
        return slots;
    }
    
    protected int getItemsPerPage() {
        return getPaginationSlots().size();
    }
    
    protected void renderPaginationNavigation(@NotNull RenderContext render, @NotNull Player player, @NotNull List<T> items) {
        int page = currentPage.get(render);
        int totalPages = (int) Math.ceil((double) items.size() / getItemsPerPage());
        boolean hasPrevious = page > 0;
        boolean hasNext = page < totalPages - 1;
        
        ItemStack previousItem = createPreviousPageItem(player, hasPrevious);
        render.layoutSlot(PREVIOUS_CHAR, previousItem)
            .onClick(click -> {
                if (hasPrevious) {
                    currentPage.set(page - 1, click);
                }
            })
            .updateOnStateChange(currentPage);
        
        ItemStack nextItem = createNextPageItem(player, hasNext);
        render.layoutSlot(NEXT_CHAR, nextItem)
            .onClick(click -> {
                if (hasNext) {
                    currentPage.set(page + 1, click);
                }
            })
            .updateOnStateChange(currentPage);
        
        ItemStack pageInfoItem = createPageInfoItem(player, page + 1, totalPages, items.size());
        render.layoutSlot(PAGE_INFO_CHAR, pageInfoItem)
            .updateOnStateChange(currentPage);
    }
    
    protected void renderAdditionalContent(@NotNull RenderContext render, @NotNull Player player) {
        // Default implementation does nothing
    }
    
    protected void handleItemClick(@NotNull Player player, @NotNull T item, int index) {
        // Default implementation does nothing
    }
    
    @NotNull
    protected ItemStack createEmptySlot(@NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
            .setName(Component.empty())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }
    
    @NotNull
    protected ItemStack createPreviousPageItem(@NotNull Player player, boolean canGoBack) {
        Material material = canGoBack ? Material.ARROW : Material.GRAY_DYE;
        
        return UnifiedBuilderFactory.item(material)
            .setName(i18n("pagination.previous", player).build().component())
            .setLore(canGoBack ? 
                i18n("pagination.previous.available", player).build().children() :
                i18n("pagination.previous.unavailable", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }
    
    @NotNull
    protected ItemStack createNextPageItem(@NotNull Player player, boolean canGoForward) {
        Material material = canGoForward ? Material.ARROW : Material.GRAY_DYE;
        
        return UnifiedBuilderFactory.item(material)
            .setName(i18n("pagination.next", player).build().component())
            .setLore(canGoForward ? 
                i18n("pagination.next.available", player).build().children() :
                i18n("pagination.next.unavailable", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }
    
    @NotNull
    protected ItemStack createPageInfoItem(@NotNull Player player, int currentPage, int totalPages, int totalItems) {
        return UnifiedBuilderFactory.item(Material.PAPER)
            .setName(i18n("pagination.info", player)
                .withPlaceholder("current", String.valueOf(currentPage))
                .withPlaceholder("total", String.valueOf(totalPages))
                .build().component())
            .setLore(i18n("pagination.info.description", player)
                .withPlaceholder("items", String.valueOf(totalItems))
                .build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }
    
    @Override
    protected void handleRefreshClick(@NotNull Player player) {
        super.handleRefreshClick(player);
    }
}
