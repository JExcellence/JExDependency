---
inclusion: always
---

# Inventory Framework - GUI Development Guide

This document provides comprehensive guidance for creating interactive GUIs using the Inventory Framework library.

## Quick Start

### Basic View Setup

```java
import me.devnatan.inventoryframework.View;
import me.devnatan.inventoryframework.ViewConfigBuilder;
import me.devnatan.inventoryframework.context.RenderContext;

public class MyView extends View {
    
    @Override
    public void onInit(ViewConfigBuilder config) {
        config.title("My GUI Title")
              .size(3)  // 3 rows (27 slots)
              .cancelOnClick();
    }
    
    @Override
    public void onRender(RenderContext render) {
        // Add items to the GUI
    }
}
```

### Opening a View

```java
// In your plugin
ViewFrame frame = ViewFrame.create(plugin)
    .with(new MyView())
    .register();

// Open for player
frame.open(MyView.class, player);
```

## RPlatform BaseView Architecture

### BaseView - Template Method Pattern

The RPlatform `BaseView` provides a template-method implementation that centralizes layout, translation, and navigation behavior. It uses the legacy I18n wrapper system for backward compatibility.

```java
public abstract class MyView extends BaseView {
    
    public MyView() {
        super(ParentView.class);  // For back navigation
        // or super() for no parent
    }
    
    @Override
    protected String getKey() {
        return "view.myview";  // Base translation key
    }
    
    @Override
    protected String[] getLayout() {
        return new String[]{
            "XXXXXXXXX",  // X = filler/decoration
            "X       X",  // Space = empty slots
            "X       X",
            "XXXXXXXXX",
            "         "   // Bottom row - back button auto-placed at slot 0
        };
    }
    
    @Override
    public void onFirstRender(RenderContext render, Player player) {
        // Your custom rendering logic
        render.slot(10)
            .withItem(itemStack -> {
                itemStack.setType(Material.DIAMOND);
                itemStack.displayName(i18n("items.diamond.name", player).build().component());
                itemStack.lore(i18n("items.diamond.lore", player).build().children());
            })
            .onClick(this::handleClick);
    }
}
```

### Key BaseView Features

1. **Automatic Back Button**: Placed at bottom-left corner (first slot of last row) automatically
2. **Layout-Based or Size-Based**: Use `getLayout()` for character-based layouts or `getSize()` for simple row count
3. **Auto-Fill**: Empty slots automatically filled with gray glass pane (configurable)
4. **I18n Integration**: Built-in `i18n(suffix, player)` method for scoped translations
5. **Parent Navigation**: Automatic back button handling with parent view support

### BaseView Methods Reference

```java
// Required overrides
protected abstract String getKey();  // Base translation key
public abstract void onFirstRender(RenderContext render, Player player);

// Optional overrides
protected String getTitleKey() { return "title"; }  // Title suffix
protected String[] getLayout() { return null; }  // Character-based layout
protected int getSize() { return 6; }  // Fallback row count
protected int getUpdateSchedule() { return 0; }  // Auto-update ticks
protected Material getFillMaterial() { return Material.GRAY_STAINED_GLASS_PANE; }
protected boolean shouldAutoFill() { return true; }
protected ItemStack createFillItem(Player player) { /* ... */ }
protected Map<String, Object> getTitlePlaceholders(OpenContext open) { return Map.of(); }

// Utility methods
protected I18n.Builder i18n(String suffix, Player player);  // Scoped translation builder
protected void handleBackButtonClick(SlotClickContext click);  // Back navigation handler
```

### Layout Character System

The layout uses single characters to define slot behavior:

- **Space (` `)**: Empty slot (back button auto-placed at bottom-left)
- **Any other char**: Custom slot that you handle via `render.layoutSlot(char, item)`
- **Common conventions**:
  - `X` - Decoration/filler
  - `O` - Content slots (used by APaginatedView)
  - `<` - Previous button (pagination)
  - `>` - Next button (pagination)
  - `p` - Page indicator (pagination)

### Example: Simple View

```java
public class ShopView extends BaseView {
    
    public ShopView() {
        super(MainMenuView.class);  // Back goes to main menu
    }
    
    @Override
    protected String getKey() {
        return "view.shop";
    }
    
    @Override
    protected String[] getLayout() {
        return new String[]{
            "XXXXXXXXX",
            "X   i   X",  // 'i' = item slots
            "X   i   X",
            "XXXXXXXXX",
            "         "   // Back button auto-placed here
        };
    }
    
    @Override
    public void onFirstRender(RenderContext render, Player player) {
        // Render decoration
        render.layoutSlot('X', createFillItem(player));
        
        // Render shop items
        render.layoutSlot('i')
            .withItem(itemStack -> {
                itemStack.setType(Material.DIAMOND);
                itemStack.displayName(i18n("items.diamond.name", player).build().component());
                itemStack.lore(i18n("items.diamond.lore", player)
                    .withPlaceholder("price", 100)
                    .build().children());
            })
            .onClick(click -> handlePurchase(click, "diamond", 100));
    }
    
    private void handlePurchase(SlotClickContext click, String item, int price) {
        Player player = click.getPlayer();
        // Purchase logic
        click.close();
    }
}
```

## Translation File Structure

```yaml
view:
  perks:
    title: "<gradient:#FF0000:#00FF00>Perks Menu</gradient>"
    items:
      close:
        name: "<red>Close</red>"
        lore:
          - "<gray>Click to close this menu</gray>"
      perk:
        name: "<aqua>{name}</aqua>"
        lore:
          - "<gray>{description}</gray>"
          - ""
          - "<yellow>Duration: {duration}s</yellow>"
          - "<yellow>Level: {level}</yellow>"
          - ""
          - "<green>Click to activate!</green>"
```

## Item Creation

### Simple Items

```java
@Override
public void onRender(RenderContext render) {
    Player player = render.getPlayer();
    
    // Simple item
    render.slot(0)
        .withItem(ItemStack.of(Material.DIAMOND))
        .onClick(click -> {
            player.sendMessage("Clicked!");
        });
}
```

### Items with i18n

```java
@Override
public void onRender(RenderContext render) {
    Player player = render.getPlayer();
    
    render.slot(10)
        .withItem(itemStack -> {
            itemStack.setType(Material.POTION);
            
            // Translated name
            Component name = r18n.message(parentKey + ".items.perk.name")
                .placeholder("name", "Speed Boost")
                .toComponent(player);
            itemStack.displayName(name);
            
            // Translated lore
            List<Component> lore = r18n.message(parentKey + ".items.perk.lore")
                .placeholder("description", "Increases movement speed")
                .placeholder("duration", 60)
                .placeholder("level", 2)
                .toComponents(player);
            itemStack.lore(lore);
        })
        .onClick(this::handlePerkClick);
}
```

### Dynamic Items

```java
public void renderPerks(RenderContext render, List<Perk> perks) {
    Player player = render.getPlayer();
    int slot = 10;
    
    for (Perk perk : perks) {
        render.slot(slot++)
            .withItem(itemStack -> {
                itemStack.setType(perk.getMaterial());
                
                Component name = r18n.message(parentKey + ".items.perk.name")
                    .placeholder("name", perk.getName())
                    .toComponent(player);
                itemStack.displayName(name);
                
                List<Component> lore = r18n.message(parentKey + ".items.perk.lore")
                    .placeholder("description", perk.getDescription())
                    .placeholder("duration", perk.getDuration())
                    .placeholder("level", perk.getLevel())
                    .toComponents(player);
                itemStack.lore(lore);
            })
            .onClick(click -> handlePerkClick(click, perk));
    }
}
```

## Click Handling

### Basic Click Handler

```java
render.slot(0)
    .withItem(ItemStack.of(Material.DIAMOND))
    .onClick(click -> {
        Player player = click.getPlayer();
        click.close();
        
        r18n.message("perk.activated")
            .placeholder("perk", "Speed")
            .send(player);
    });
```

### Click with Data

```java
private void handlePerkClick(ClickContext click, Perk perk) {
    Player player = click.getPlayer();
    
    // Check if player can afford
    if (!canAfford(player, perk.getCost())) {
        r18n.message("error.insufficient-funds")
            .placeholder("cost", perk.getCost())
            .withPrefix()
            .send(player);
        return;
    }
    
    // Activate perk
    perk.activate(player);
    click.close();
    
    r18n.message("perk.activated")
        .placeholder("perk", perk.getName())
        .withPrefix()
        .send(player);
}
```

### Async Click Handling

```java
render.slot(0)
    .withItem(ItemStack.of(Material.CHEST))
    .onClick(click -> {
        Player player = click.getPlayer();
        
        // Run async operation
        CompletableFuture.supplyAsync(() -> {
            return database.loadPlayerData(player.getUniqueId());
        }).thenAccept(data -> {
            // Back to main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                // Update GUI or send message
                r18n.message("data.loaded")
                    .placeholder("value", data.getValue())
                    .send(player);
            });
        });
    });
```

## APaginatedView - Async Pagination

### Overview

`APaginatedView<T>` extends `BaseView` with built-in pagination support, async data loading, and automatic navigation controls using custom player heads.

### Key Features

1. **Async Data Loading**: Uses `CompletableFuture` for non-blocking data retrieval
2. **Automatic Navigation**: Previous/Next buttons with custom heads
3. **Page Indicator**: Shows current page with numbered heads (0-9) or paper fallback
4. **Layout Integration**: Uses character-based layout with pagination slots
5. **Type-Safe**: Generic type parameter for paginated items

### Basic Implementation

```java
public class PerkListView extends APaginatedView<Perk> {
    private final PerkService perkService;
    
    public PerkListView(PerkService perkService) {
        super(MainMenuView.class);  // Parent for back button
        this.perkService = perkService;
    }
    
    @Override
    protected String getKey() {
        return "view.perks";
    }
    
    @Override
    protected CompletableFuture<List<Perk>> getAsyncPaginationSource(Context context) {
        Player player = context.getPlayer();
        // Load perks asynchronously
        return perkService.getAvailablePerks(player.getUniqueId());
    }
    
    @Override
    protected void renderEntry(
        Context context,
        BukkitItemComponentBuilder builder,
        int index,
        Perk perk
    ) {
        Player player = context.getPlayer();
        
        builder
            .withItem(itemStack -> {
                itemStack.setType(perk.getMaterial());
                itemStack.displayName(i18n("items.perk.name", player)
                    .withPlaceholder("name", perk.getName())
                    .build().component());
                itemStack.lore(i18n("items.perk.lore", player)
                    .withPlaceholder("description", perk.getDescription())
                    .withPlaceholder("duration", perk.getDuration())
                    .withPlaceholder("level", perk.getLevel())
                    .build().children());
            })
            .onClick(click -> handlePerkClick(click, perk));
    }
    
    @Override
    protected void onPaginatedRender(RenderContext render, Player player) {
        // Optional: Add extra UI elements beyond pagination
        // This is called after pagination navigation is set up
    }
    
    private void handlePerkClick(SlotClickContext click, Perk perk) {
        Player player = click.getPlayer();
        // Handle perk activation
        perk.activate(player);
        click.close();
    }
}
```

### Default Pagination Layout

```java
@Override
protected String[] getLayout() {
    return new String[]{
        "XXXXXXXXX",  // X = decoration
        "XOOOOOOOX",  // O = pagination content slots
        "XOOOOOOOX",
        "XOOOOOOOX",
        "XXXXXXXXX",
        "   <p>   "   // < = previous, p = page indicator, > = next
    };
}
```

### Customizing Layout Characters

```java
@Override
protected char getPaginationSlotChar() {
    return 'O';  // Default: 'O'
}

@Override
protected char getPreviousButtonChar() {
    return '<';  // Default: '<'
}

@Override
protected char getNextButtonChar() {
    return '>';  // Default: '>'
}

@Override
protected char getPageIndicatorChar() {
    return 'p';  // Default: 'p'
}
```

### Custom Layout Example

```java
public class CompactPerkView extends APaginatedView<Perk> {
    
    @Override
    protected String[] getLayout() {
        return new String[]{
            "OOOOOOOOO",  // Full row of items
            "OOOOOOOOO",
            "OOOOOOOOO",
            "   <p>   "   // Navigation at bottom
        };
    }
    
    @Override
    protected String getKey() {
        return "view.perks.compact";
    }
    
    // ... rest of implementation
}
```

### Accessing Pagination State

```java
@Override
protected void onPaginatedRender(RenderContext render, Player player) {
    Pagination pagination = getPagination(render);
    
    int currentPage = pagination.currentPageIndex();
    int totalPages = pagination.lastPageIndex() + 1;
    int itemCount = pagination.source().size();
    
    // Use pagination info for custom UI elements
    render.slot(4)
        .withItem(itemStack -> {
            itemStack.setType(Material.BOOK);
            itemStack.displayName(i18n("items.info.name", player)
                .withPlaceholder("items", itemCount)
                .build().component());
        });
}
```

### Advanced: Filtered Pagination

```java
public class FilteredPerkView extends APaginatedView<Perk> {
    private final PerkService perkService;
    
    @Override
    protected CompletableFuture<List<Perk>> getAsyncPaginationSource(Context context) {
        Player player = context.getPlayer();
        String filter = context.get("filter", "all");
        
        return perkService.getAvailablePerks(player.getUniqueId())
            .thenApply(perks -> filterPerks(perks, filter));
    }
    
    @Override
    protected void onPaginatedRender(RenderContext render, Player player) {
        // Add filter buttons
        String currentFilter = render.get("filter", "all");
        
        render.slot(0)
            .withItem(itemStack -> {
                itemStack.setType(Material.REDSTONE);
                itemStack.displayName(i18n("filter.all.name", player).build().component());
            })
            .onClick(click -> {
                click.set("filter", "all");
                // Trigger pagination reload
                getPagination(click).switchPage(0);
            });
        
        render.slot(1)
            .withItem(itemStack -> {
                itemStack.setType(Material.DIAMOND);
                itemStack.displayName(i18n("filter.active.name", player).build().component());
            })
            .onClick(click -> {
                click.set("filter", "active");
                getPagination(click).switchPage(0);
            });
    }
    
    private List<Perk> filterPerks(List<Perk> perks, String filter) {
        return switch (filter) {
            case "active" -> perks.stream().filter(Perk::isActive).toList();
            case "inactive" -> perks.stream().filter(p -> !p.isActive()).toList();
            default -> perks;
        };
    }
}
```

### APaginatedView Methods Reference

```java
// Required overrides
protected abstract CompletableFuture<List<T>> getAsyncPaginationSource(Context context);
protected abstract void renderEntry(Context context, BukkitItemComponentBuilder builder, int index, T entry);
protected abstract void onPaginatedRender(RenderContext render, Player player);

// Optional overrides (layout characters)
protected char getPaginationSlotChar() { return 'O'; }
protected char getPreviousButtonChar() { return '<'; }
protected char getNextButtonChar() { return '>'; }
protected char getPageIndicatorChar() { return 'p'; }

// Utility methods
protected final Pagination getPagination(RenderContext context);
```

## Context Data

### Storing Data in Context

```java
@Override
public void onRender(RenderContext render) {
    // Store data
    render.set("selectedPerk", perk);
    render.set("page", 0);
    render.set("filter", "active");
}
```

### Retrieving Data

```java
private void handleClick(ClickContext click) {
    // Get stored data
    Perk perk = click.get("selectedPerk");
    int page = click.get("page", 0);  // With default
    
    // Use data
    if (perk != null) {
        perk.activate(click.getPlayer());
    }
}
```

## View Lifecycle

### Lifecycle Methods

```java
public class MyView extends BaseView {
    
    @Override
    public void onInit(ViewConfigBuilder config) {
        // Called once when view is registered
        config.title("My View").size(3);
    }
    
    @Override
    public void onOpen(OpenContext context) {
        // Called when view is opened for a player
        Player player = context.getPlayer();
        getLogger().info(player.getName() + " opened view");
    }
    
    @Override
    public void onFirstRender(RenderContext render) {
        // Called on first render only
        // Good for static items
        renderStaticItems(render);
    }
    
    @Override
    public void onRender(RenderContext render) {
        // Called every time view is rendered
        // Good for dynamic items
        renderDynamicItems(render);
    }
    
    @Override
    public void onUpdate(UpdateContext context) {
        // Called when render.update() is called
        refreshItems(context);
    }
    
    @Override
    public void onClose(CloseContext context) {
        // Called when view is closed
        Player player = context.getPlayer();
        getLogger().info(player.getName() + " closed view");
    }
}
```

## Confirmation Dialogs

### Simple Confirmation

```java
public class ConfirmationView extends BaseView {
    private final Runnable onConfirm;
    private final Runnable onCancel;
    
    public ConfirmationView(R18nManager r18n, Runnable onConfirm, Runnable onCancel) {
        super(r18n, "view.confirm");
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
    }
    
    @Override
    public void onInit(ViewConfigBuilder config) {
        config.size(3).cancelOnClick();
    }
    
    @Override
    public void onRender(RenderContext render) {
        Player player = render.getPlayer();
        
        // Confirm button (green)
        render.slot(11)
            .withItem(itemStack -> {
                itemStack.setType(Material.GREEN_WOOL);
                itemStack.displayName(getMessage("items.confirm.name", player));
                itemStack.lore(getMessages("items.confirm.lore", player));
            })
            .onClick(click -> {
                click.close();
                onConfirm.run();
            });
        
        // Cancel button (red)
        render.slot(15)
            .withItem(itemStack -> {
                itemStack.setType(Material.RED_WOOL);
                itemStack.displayName(getMessage("items.cancel.name", player));
                itemStack.lore(getMessages("items.cancel.lore", player));
            })
            .onClick(click -> {
                click.close();
                if (onCancel != null) {
                    onCancel.run();
                }
            });
    }
}
```

### Usage

```java
// Open confirmation dialog
frame.open(ConfirmationView.class, player, 
    () -> {
        // On confirm
        purchasePerk(player, perk);
    },
    () -> {
        // On cancel
        r18n.message("purchase.cancelled").send(player);
    }
);
```

## Best Practices for RPlatform Views

### 1. Always Extend BaseView or APaginatedView

```java
// ✅ Good: Use BaseView for simple views
public class ShopView extends BaseView {
    public ShopView() {
        super(MainMenuView.class);
    }
    
    @Override
    protected String getKey() {
        return "view.shop";
    }
}

// ✅ Good: Use APaginatedView for lists
public class PerkListView extends APaginatedView<Perk> {
    public PerkListView() {
        super(MainMenuView.class);
    }
    
    @Override
    protected String getKey() {
        return "view.perks";
    }
}

// ❌ Bad: Extending View directly
public class MyView extends View {
    // Missing all BaseView benefits
}
```

### 2. Use Layout System for Complex UIs

```java
// ✅ Good: Character-based layout
@Override
protected String[] getLayout() {
    return new String[]{
        "XXXXXXXXX",
        "X       X",
        "XXXXXXXXX",
        "         "
    };
}

// ❌ Bad: Manual slot positioning
@Override
public void onFirstRender(RenderContext render, Player player) {
    render.slot(0).withItem(...);  // Hard to maintain
    render.slot(1).withItem(...);
    render.slot(2).withItem(...);
    // ... 50 more lines
}
```

### 3. Always Provide Parent for Navigation

```java
// ✅ Good: Parent specified for back button
public class DetailView extends BaseView {
    public DetailView() {
        super(ListingView.class);  // Back goes to listing
    }
}

// ⚠️ Acceptable: No parent closes inventory
public class MainMenuView extends BaseView {
    public MainMenuView() {
        super();  // Back button closes inventory
    }
}
```

### 4. Use i18n() Helper Method

```java
// ✅ Good: Use BaseView's i18n helper
@Override
public void onFirstRender(RenderContext render, Player player) {
    render.slot(10)
        .withItem(itemStack -> {
            itemStack.displayName(i18n("items.sword.name", player)
                .withPlaceholder("damage", 10)
                .build().component());
            itemStack.lore(i18n("items.sword.lore", player)
                .withPlaceholder("durability", 100)
                .build().children());
        });
}

// ❌ Bad: Manual key construction
@Override
public void onFirstRender(RenderContext render, Player player) {
    String fullKey = getKey() + ".items.sword.name";  // Error-prone
    Component name = new I18n.Builder(fullKey, player).build().component();
}
```

### 5. Handle Async Operations in APaginatedView

```java
// ✅ Good: Return CompletableFuture
@Override
protected CompletableFuture<List<Perk>> getAsyncPaginationSource(Context context) {
    return perkService.loadPerksAsync(context.getPlayer().getUniqueId());
}

// ❌ Bad: Blocking call
@Override
protected CompletableFuture<List<Perk>> getAsyncPaginationSource(Context context) {
    List<Perk> perks = perkService.loadPerksSync();  // Blocks server!
    return CompletableFuture.completedFuture(perks);
}
```

### 6. Don't Override onFirstRender() in APaginatedView

```java
// ❌ Bad: Overriding onFirstRender breaks pagination
public class MyPaginatedView extends APaginatedView<Item> {
    @Override
    public void onFirstRender(RenderContext render, Player player) {
        // This breaks pagination setup!
        super.onFirstRender(render, player);
        // Your code
    }
}

// ✅ Good: Use onPaginatedRender instead
public class MyPaginatedView extends APaginatedView<Item> {
    @Override
    protected void onPaginatedRender(RenderContext render, Player player) {
        // Add extra UI elements here
        // Pagination is already set up
    }
}
```

### 7. Auto-Fill Behavior

```java
// Auto-fill is enabled by default
@Override
protected boolean shouldAutoFill() {
    return true;  // Default
}

// Customize fill material
@Override
protected Material getFillMaterial() {
    return Material.BLACK_STAINED_GLASS_PANE;  // Default: GRAY
}

// Disable auto-fill for full control
@Override
protected boolean shouldAutoFill() {
    return false;  // Manual slot management
}
```

## RPlatform-Specific Features

### Custom Player Heads

RPlatform includes a head utility library with pre-configured heads:

```java
import com.raindropcentral.rplatform.utility.heads.view.Return;
import com.raindropcentral.rplatform.utility.heads.view.Next;
import com.raindropcentral.rplatform.utility.heads.view.Previous;

// Used automatically by BaseView and APaginatedView
// Return head: Back button (bottom-left)
// Next head: Next page button
// Previous head: Previous page button
// Number0-9 heads: Page indicators
```

### UnifiedBuilderFactory

Use for cross-version item building:

```java
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;

ItemStack item = UnifiedBuilderFactory.item(Material.DIAMOND_SWORD)
    .setName(Component.text("Epic Sword"))
    .setLore(List.of(Component.text("A legendary weapon")))
    .build();
```

### Automatic Back Button Placement

The back button is automatically placed at the bottom-left corner (first slot of last row):

```java
// Layout with 6 rows
@Override
protected String[] getLayout() {
    return new String[]{
        "XXXXXXXXX",  // Row 1
        "X       X",  // Row 2
        "X       X",  // Row 3
        "X       X",  // Row 4
        "XXXXXXXXX",  // Row 5
        "         "   // Row 6 - Back button at slot 45 (first slot)
    };
}

// If slot is occupied by layout character, back button won't be placed
@Override
protected String[] getLayout() {
    return new String[]{
        "XXXXXXXXX",
        "X       X",
        "bXXXXXXXX"   // 'b' occupies slot 45, no auto back button
    };
}
```

### View Lifecycle in RPlatform

```java
public class MyView extends BaseView {
    
    // 1. Constructor called
    public MyView() {
        super(ParentView.class);
    }
    
    // 2. onInit called once during registration
    @Override
    public void onInit(ViewConfigBuilder config) {
        super.onInit(config);  // Sets up layout/size
    }
    
    // 3. onOpen called when view opens for player
    @Override
    public void onOpen(OpenContext open) {
        super.onOpen(open);  // Sets translated title
    }
    
    // 4. onFirstRender called for initial render
    @Override
    public void onFirstRender(RenderContext render, Player player) {
        // Your rendering logic
    }
    
    // 5. onClose called when player closes inventory
    @Override
    public void onClose(CloseContext context) {
        super.onClose(context);
        // Cleanup logic
    }
}
```

## Common Patterns with RPlatform Views

### Detail View with Back Navigation

```java
public class PerkDetailView extends BaseView {
    private final Perk perk;
    
    public PerkDetailView(Perk perk) {
        super(PerkListView.class);  // Back to list
        this.perk = perk;
    }
    
    @Override
    protected String getKey() {
        return "view.perk.detail";
    }
    
    @Override
    protected Map<String, Object> getTitlePlaceholders(OpenContext open) {
        return Map.of("perk", perk.getName());
    }
    
    @Override
    public void onFirstRender(RenderContext render, Player player) {
        // Display perk details
        render.slot(13)
            .withItem(itemStack -> {
                itemStack.setType(perk.getMaterial());
                itemStack.displayName(i18n("items.perk.name", player)
                    .withPlaceholder("name", perk.getName())
                    .build().component());
                itemStack.lore(i18n("items.perk.lore", player)
                    .withPlaceholder("description", perk.getDescription())
                    .withPlaceholder("duration", perk.getDuration())
                    .build().children());
            });
        
        // Activate button
        render.slot(22)
            .withItem(itemStack -> {
                itemStack.setType(Material.GREEN_WOOL);
                itemStack.displayName(i18n("items.activate.name", player).build().component());
            })
            .onClick(click -> {
                perk.activate(player);
                click.close();
            });
    }
}
```

### Confirmation Dialog

```java
public class ConfirmPurchaseView extends BaseView {
    private final Runnable onConfirm;
    
    public ConfirmPurchaseView(Class<? extends View> parent, Runnable onConfirm) {
        super(parent);
        this.onConfirm = onConfirm;
    }
    
    @Override
    protected String getKey() {
        return "view.confirm";
    }
    
    @Override
    protected int getSize() {
        return 3;  // Small dialog
    }
    
    @Override
    public void onFirstRender(RenderContext render, Player player) {
        // Confirm button
        render.slot(11)
            .withItem(itemStack -> {
                itemStack.setType(Material.GREEN_WOOL);
                itemStack.displayName(i18n("items.confirm.name", player).build().component());
            })
            .onClick(click -> {
                onConfirm.run();
                click.close();
            });
        
        // Cancel button (back button handles this automatically)
        render.slot(15)
            .withItem(itemStack -> {
                itemStack.setType(Material.RED_WOOL);
                itemStack.displayName(i18n("items.cancel.name", player).build().component());
            })
            .onClick(SlotClickContext::close);
    }
}

// Usage
frame.open(ConfirmPurchaseView.class, player, 
    ShopView.class,  // Parent
    () -> purchaseItem(player, item)  // On confirm
);
```

```java
@Override
public void onFirstRender(RenderContext render) {
    // Static items (borders, navigation, etc.)
    renderBorders(render);
    renderNavigation(render);
}

@Override
public void onRender(RenderContext render) {
    // Dynamic items (data-driven content)
    renderPerks(render);
}
```

### 3. Use Context for State

```java
// ✅ Good: Store state in context
render.set("page", currentPage);
render.set("filter", selectedFilter);

// ❌ Bad: Instance variables (not player-specific)
private int currentPage;  // Shared across all players!
```

### 4. Handle Async Operations Properly

```java
// ✅ Good: Async with proper thread handling
render.slot(0)
    .withItem(ItemStack.of(Material.CHEST))
    .onClick(click -> {
        CompletableFuture.supplyAsync(() -> database.load())
            .thenAccept(data -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    // Update GUI on main thread
                    click.update();
                });
            });
    });

// ❌ Bad: Blocking main thread
render.slot(0)
    .onClick(click -> {
        Data data = database.load();  // Blocks server!
    });
```

### 5. Clean Up Resources

```java
@Override
public void onClose(CloseContext context) {
    // Clean up any resources
    Player player = context.getPlayer();
    activeViews.remove(player.getUniqueId());
    
    // Cancel any pending tasks
    if (updateTask != null) {
        updateTask.cancel();
    }
}
```

## Common Patterns

### Loading Screen

```java
public class LoadingView extends BaseView {
    
    @Override
    public void onRender(RenderContext render) {
        Player player = render.getPlayer();
        
        // Show loading indicator
        render.slot(13)
            .withItem(itemStack -> {
                itemStack.setType(Material.HOPPER);
                itemStack.displayName(getMessage("items.loading.name", player));
            });
        
        // Load data async
        CompletableFuture.supplyAsync(() -> loadData(player))
            .thenAccept(data -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    // Open actual view with data
                    frame.open(DataView.class, player, data);
                });
            });
    }
}
```

### Search/Filter View

```java
public class FilteredView extends BaseView {
    
    @Override
    public void onRender(RenderContext render) {
        String filter = render.get("filter", "all");
        List<Perk> filtered = filterPerks(allPerks, filter);
        
        renderPerks(render, filtered);
        renderFilterButtons(render, filter);
    }
    
    private void renderFilterButtons(RenderContext render, String currentFilter) {
        Player player = render.getPlayer();
        
        render.slot(0)
            .withItem(itemStack -> {
                itemStack.setType(Material.REDSTONE);
                itemStack.displayName(getMessage("filter.all.name", player));
            })
            .onClick(click -> {
                click.set("filter", "all");
                click.update();
            });
        
        // More filter buttons...
    }
}
```

## Translation Keys Structure for RPlatform Views

### BaseView Translation Structure

```yaml
view:
  myview:
    title: "<gradient:#FF0000:#00FF00>My View Title</gradient>"
    items:
      diamond:
        name: "<aqua>Diamond Item</aqua>"
        lore:
          - "<gray>This is a diamond item</gray>"
          - "<yellow>Price: {price} coins</yellow>"
```

### APaginatedView Translation Structure

The page indicator uses special placeholders that are automatically provided:

```yaml
# Global page indicator translations (used by all paginated views)
page:
  name: "<aqua>Page {current_page}/{total_pages}</aqua>"
  lore:
    - "<gray>Showing items {first_item}-{last_item} of {items_count}</gray>"
    - ""
    - "<yellow>◀ Previous | Next ▶</yellow>"
  fallback: "<gray>Page {page}</gray>"  # Used when numbered head not available

# View-specific translations
view:
  perks:
    title: "<gradient:#FF0000:#00FF00>Perks - Page {page}/{max_page}</gradient>"
    items:
      perk:
        name: "<gradient:#00FF00:#00FFFF>{name}</gradient>"
        lore:
          - "<gray>{description}</gray>"
          - ""
          - "<yellow>Duration: {duration}s</yellow>"
          - "<yellow>Level: {level}</yellow>"
          - ""
          - "<green>▶ Click to activate</green>"
      filter:
        all:
          name: "<white>All Perks</white>"
        active:
          name: "<green>Active Perks</green>"
        inactive:
          name: "<red>Inactive Perks</red>"
```

### Page Indicator Placeholders

APaginatedView automatically provides these placeholders for page indicators:

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `{page}` | Current page (1-indexed) | `1` |
| `{max_page}` | Total number of pages | `5` |
| `{current_page}` | Alias for `{page}` | `1` |
| `{total_pages}` | Alias for `{max_page}` | `5` |
| `{first_page}` | Always `1` | `1` |
| `{items_count}` | Total items across all pages | `42` |

### Title Placeholders

Override `getTitlePlaceholders()` to provide custom placeholders:

```java
@Override
protected Map<String, Object> getTitlePlaceholders(OpenContext open) {
    return Map.of(
        "player", open.getPlayer().getName(),
        "server", Bukkit.getServer().getName()
    );
}
```

Then use in translation:

```yaml
view:
  myview:
    title: "<gold>{player}'s View on {server}</gold>"
```

### Confirmation Dialog Structure

```yaml
view:
  confirm:
    title: "<red>Confirm Purchase</red>"
    items:
      confirm:
        name: "<green>✓ Confirm</green>"
        lore:
          - "<gray>Click to confirm purchase</gray>"
          - "<yellow>Cost: {cost} coins</yellow>"
      cancel:
        name: "<red>✗ Cancel</red>"
        lore:
          - "<gray>Click to cancel</gray>"
```

## Related Documentation

- JExTranslate i18n: See `jextranslate-i18n.md`
- PaperMC Best Practices: See `papermc-best-practices.md`
- Inventory Framework Docs: https://github.com/devnatan/inventory-framework
