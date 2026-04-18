---
inclusion: always
---
# JExTranslate I18n System - Development Guide

This steering document provides comprehensive guidance for using the JExTranslate i18n system across all projects in this repository.

## Quick Reference

### Initialize R18nManager

```java
public class MyPlugin extends JavaPlugin {
    private R18nManager r18n;

    @Override
    public void onEnable() {
        r18n = R18nManager.builder(this)
            .defaultLocale("en_US")
            .supportedLocales("en_US", "de_DE", "es_ES", "fr_FR")
            .enableKeyValidation(true)
            .enablePlaceholderAPI(true)
            .build();

        r18n.initialize().thenRun(() -> {
            getLogger().info("R18n initialized!");
            r18n.registerCommand(); // Register /r18n command
        });
    }

    @Override
    public void onDisable() {
        if (r18n != null) {
            r18n.shutdown();
        }
    }
}
```

### Send Messages

```java
// Simple message
r18n.message("welcome.player")
    .placeholder("player", player.getName())
    .send(player);

// With prefix
r18n.message("error.permission")
    .placeholder("permission", "admin.reload")
    .withPrefix()
    .send(player);

// Broadcast
r18n.message("server.announcement")
    .placeholder("message", "Restarting soon!")
    .broadcast();

// Console
r18n.message("server.startup").console();

// Explicit locale
r18n.message("welcome.player")
    .locale("de_DE")
    .send(player);
```

### Get Components for GUIs

```java
// Single component for item names
Component itemName = r18n.message("item.sword.name")
    .placeholder("damage", 10)
    .toComponent(player);

// Multiple components for lore
List<Component> lore = r18n.message("item.sword.lore")
    .placeholder("damage", 10)
    .placeholder("durability", 100)
    .toComponents(player);

// Plain string for placeholders
String value = r18n.message("placeholder.value")
    .toString(player);
```

## Translation File Structure

### Directory Layout

```
plugins/YourPlugin/
└── src/main/resources/translations/
    ├── en_US.yml          # English (US) - REQUIRED
    ├── de_DE.yml          # German
    ├── es_ES.yml          # Spanish
    ├── fr_FR.yml          # French
    └── ja_JP.yml          # Japanese
```

### File Format (YAML)

```yaml
# Prefix for all messages (optional)
prefix: "<gold>[PluginName]</gold> "

# Use dot notation for nested keys
welcome:
  player: "<green>Welcome, {player}!</green>"
  first-join:
    - "<gold>Welcome to the server!</gold>"
    - "<gray>Type /help to get started.</gray>"

# Error messages
error:
  permission: "<red>No permission: {permission}</red>"
  not-found: "<red>Not found: {item}</red>"

# GUI items
gui:
  items:
    close:
      name: "<red>Close</red>"
      lore:
        - "<gray>Click to close this menu</gray>"
    next:
      name: "<green>Next Page</green>"
      lore:
        - "<gray>Page {current}/{total}</gray>"

# Plural support
items:
  count:
    one: "You have {count} item"
    other: "You have {count} items"
```

## Placeholder Formats

Both formats are supported:
- `{placeholder}` - Brace format (recommended)
- `%placeholder%` - Percent format

## MiniMessage Formatting

Full MiniMessage support:

```yaml
# Colors
basic: "<red>Red text</red>"
hex: "<#FF5500>Custom color</color>"

# Gradients
gradient: "<gradient:#FF0000:#00FF00>Rainbow text</gradient>"

# Decorations
bold: "<bold>Bold text</bold>"
italic: "<italic>Italic text</italic>"
underlined: "<underlined>Underlined</underlined>"
strikethrough: "<strikethrough>Strikethrough</strikethrough>"

# Click events
clickable: "<click:run_command:/help>Click here</click>"
suggest: "<click:suggest_command:/msg >Message someone</click>"
url: "<click:open_url:'https://example.com'>Visit website</click>"

# Hover events
hover: "<hover:show_text:'Tooltip text'>Hover me</hover>"

# Combined
complex: "<gradient:#FF0000:#00FF00><bold><hover:show_text:'Click to teleport'><click:run_command:/spawn>Spawn</click></hover></bold></gradient>"
```

## Plural Support

Use `.count()` for grammatically correct plurals:

```java
// Translation file
items:
  count:
    one: "You have {count} item"
    other: "You have {count} items"

// Code
r18n.message("items.count")
    .count("count", itemCount)
    .send(player);
```

### Plural Forms by Language

| Language | Forms | Example |
|----------|-------|---------|
| English, German | one, other | 1 item, 2 items |
| Russian, Polish | one, few, many | 1, 2-4, 5+ |
| Arabic | zero, one, two, few, many, other | All forms |

## Key Naming Conventions

### Use Hierarchical Structure

```yaml
# Good - organized by feature
gui:
  perks:
    title: "Perks Menu"
    item:
      name: "Perk: {name}"
      lore: "Click to activate"

# Bad - flat structure
gui_perks_title: "Perks Menu"
gui_perks_item_name: "Perk: {name}"
```

### Use Descriptive Names

```yaml
# Good
error:
  insufficient-funds: "Not enough coins!"
  
# Bad
error:
  e1: "Not enough coins!"
```

### Group Related Keys

```yaml
# Group by feature/view
view:
  perks:
    title: "Perks"
    subtitle: "Select a perk"
    items:
      potion:
        name: "Potion Perk"
        lore: "Grants potion effects"
```

## Parent Keys and BaseView

When using BaseView or similar view systems, respect the parent key structure:

```java
// BaseView with parent key
public abstract class BaseView extends View {
    protected final String parentKey;
    
    public BaseView(String parentKey) {
        this.parentKey = parentKey;
    }
    
    protected Component getMessage(String subKey) {
        return r18n.message(parentKey + "." + subKey)
            .toComponent(getViewer());
    }
}

// Implementation
public class PerkView extends BaseView {
    public PerkView() {
        super("view.perks");
    }
    
    @Override
    public void onRender(ViewContext context) {
        // Uses "view.perks.title"
        context.setTitle(getMessage("title"));
        
        // Uses "view.perks.items.close.name"
        Component itemName = getMessage("items.close.name");
    }
}
```

### Translation File Structure for Views

```yaml
view:
  perks:
    title: "<gradient:#FF0000:#00FF00>Perks Menu</gradient>"
    subtitle: "<gray>Select a perk to activate</gray>"
    items:
      close:
        name: "<red>Close</red>"
        lore:
          - "<gray>Click to close</gray>"
      potion:
        name: "<aqua>Potion Perk</aqua>"
        lore:
          - "<gray>Grants potion effects</gray>"
          - ""
          - "<yellow>Duration: {duration}s</yellow>"
          - "<yellow>Level: {level}</yellow>"
```

## GUI Integration Best Practices

### Item Names and Lore

```java
// Get translated item name
Component name = r18n.message("gui.items.sword.name")
    .placeholder("damage", 10)
    .toComponent(player);

// Get translated lore (multi-line)
List<Component> lore = r18n.message("gui.items.sword.lore")
    .placeholder("damage", 10)
    .placeholder("durability", 100)
    .toComponents(player);

// Apply to ItemStack
ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
ItemMeta meta = item.getItemMeta();
meta.displayName(name);
meta.lore(lore);
item.setItemMeta(meta);
```

### Dynamic GUI Titles

```java
// Paginated view title
Component title = r18n.message("gui.perks.title")
    .placeholder("page", currentPage)
    .placeholder("total", totalPages)
    .toComponent(player);

context.setTitle(title);
```

### Confirmation Messages

```java
// Confirmation dialog
r18n.message("gui.confirm.purchase")
    .placeholder("item", itemName)
    .placeholder("cost", cost)
    .send(player);
```

## Adding New Translations

### Step 1: Add to en_US.yml (Required)

```yaml
# Always add to English first
new:
  feature:
    message: "<green>New feature message</green>"
```

### Step 2: Add to Other Locales

```yaml
# de_DE.yml
new:
  feature:
    message: "<green>Neue Funktionsnachricht</green>"

# es_ES.yml
new:
  feature:
    message: "<green>Mensaje de nueva función</green>"
```

### Step 3: Use in Code

```java
r18n.message("new.feature.message").send(player);
```

## Validation and Testing

### Enable Key Validation

```java
R18nManager r18n = R18nManager.builder(this)
    .enableKeyValidation(true)  // Validates on load
    .build();
```

### Check Missing Keys

```
/r18n missing           # Interactive browser
/r18n missing de_DE     # Check specific locale
```

### View Metrics

```
/r18n metrics           # Usage statistics
```

## Common Patterns

### Error Messages

```java
// Permission error
r18n.message("error.permission")
    .placeholder("permission", "admin.reload")
    .withPrefix()
    .send(player);

// Not found error
r18n.message("error.not-found")
    .placeholder("item", itemName)
    .withPrefix()
    .send(player);
```

### Success Messages

```java
// Purchase success
r18n.message("success.purchase")
    .placeholder("item", itemName)
    .placeholder("cost", cost)
    .withPrefix()
    .send(player);
```

### GUI Navigation

```java
// Previous page button
Component prevName = r18n.message("gui.navigation.previous")
    .toComponent(player);

// Next page button
Component nextName = r18n.message("gui.navigation.next")
    .toComponent(player);

// Page indicator
Component pageInfo = r18n.message("gui.navigation.page")
    .placeholder("current", currentPage)
    .placeholder("total", totalPages)
    .toComponent(player);
```

## Performance Considerations

### Caching

R18n automatically caches parsed Components. No manual caching needed.

```java
// This is cached automatically
Component component = r18n.message("frequently.used.key")
    .toComponent(player);
```

### Async Loading

Always initialize asynchronously:

```java
r18n.initialize().thenRun(() -> {
    // R18n ready
});
```

### Reload

```java
// Reload translations (clears cache)
r18n.reload().thenRun(() -> {
    getLogger().info("Reloaded!");
});
```

## Bedrock Edition Support

R18n automatically detects Bedrock players and converts formatting:

```java
// Automatically converts for Bedrock
r18n.message("welcome.player")
    .send(player);  // Hex colors → legacy colors for Bedrock

// Explicit Bedrock string
String bedrockText = r18n.message("item.name")
    .toBedrockString(player);

// Plain text (for forms)
String plainText = r18n.message("form.text")
    .toPlainString(player);
```

## Migration from Legacy Systems

### From Old I18n

```java
// Old
I18n.message("key", player).send();

// New
r18n.message("key").send(player);
```

### From Direct Messages

```java
// Old
player.sendMessage("§aWelcome!");

// New
r18n.message("welcome.player").send(player);
```

## Troubleshooting

### Missing Key Warnings

```
[WARNING] Missing translation: welcome.player for locale: de_DE
```

**Solution**: Add the key to de_DE.yml or use `/r18n missing de_DE`

### Placeholder Not Replaced

```yaml
# Wrong
message: "Welcome {player}"  # Missing quotes

# Correct
message: "Welcome {player}"
```

### MiniMessage Not Parsing

```yaml
# Wrong - YAML interprets < as special
message: <red>Text</red>

# Correct - Use quotes
message: "<red>Text</red>"
```

## Best Practices Summary

1. ✅ Always add translations to en_US.yml first
2. ✅ Use hierarchical key structure (dot notation)
3. ✅ Use MiniMessage for all formatting
4. ✅ Respect parent keys in BaseView implementations
5. ✅ Use `.toComponent()` for GUI items
6. ✅ Use `.toComponents()` for multi-line lore
7. ✅ Enable key validation during development
8. ✅ Use `/r18n missing` to find untranslated keys
9. ✅ Use `.count()` for plural-aware messages
10. ✅ Initialize R18n asynchronously
11. ✅ Call `shutdown()` in onDisable()
12. ✅ Quote all YAML strings with MiniMessage tags

## Related Documentation

- Full API: `JExTranslate/README.md`
- MiniMessage: https://docs.advntr.dev/minimessage/format.html
- Inventory Framework: See `inventory-framework.md`
- PaperMC Best Practices: See `papermc-best-practices.md`