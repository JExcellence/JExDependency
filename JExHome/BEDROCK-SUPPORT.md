# Bedrock Edition Support

JExHome provides native support for Minecraft Bedrock Edition players connecting via Geyser/Floodgate. Bedrock players automatically receive optimized native forms instead of chest-based GUIs.

## Requirements

To enable Bedrock support, you need:

1. **Geyser** - A proxy that allows Bedrock players to join Java servers
   - Download: https://geysermc.org/download
   - Can run as a plugin or standalone proxy

2. **Floodgate** - Allows Bedrock players to join without Java accounts
   - Download: https://geysermc.org/download#floodgate
   - Must be installed on the same server as JExHome

JExHome uses the `GeyserService` from RPlatform to detect Bedrock players. If Floodgate is not installed, all players will use the standard chest GUI.

## Architecture

The Bedrock support is implemented through RPlatform's `GeyserService`:

```
RPlatform
└── integration/geyser/
    ├── GeyserService.java      # Main service for Bedrock detection
    └── FloodgateAdapter.java   # Isolated Floodgate API wrapper
```

This design:
- Avoids static state and uses proper dependency injection
- Isolates Floodgate API calls to prevent ClassNotFoundException
- Can be reused by other plugins in the ecosystem

## Configuration

Bedrock support is configured in `plugins/JExHome/configs/home-system.yml`:

```yaml
# Bedrock Edition support settings
bedrock:
  # Enable Bedrock Forms for Bedrock players
  # When enabled, Bedrock players will see native forms instead of chest GUIs
  enabled: true
  
  # Force all players to use chest GUI (ignores Bedrock detection)
  # Set to true if you want a consistent experience for all players
  forceChestGui: false
```

### Configuration Options

| Option | Default | Description |
|--------|---------|-------------|
| `bedrock.enabled` | `true` | Enable/disable Bedrock form support entirely |
| `bedrock.forceChestGui` | `false` | Force all players (including Bedrock) to use chest GUI |

### Common Configurations

**Default (Recommended):**
```yaml
bedrock:
  enabled: true
  forceChestGui: false
```
Bedrock players see native forms, Java players see chest GUIs.

**Disable Bedrock Forms:**
```yaml
bedrock:
  enabled: false
  forceChestGui: false
```
All players use chest GUIs regardless of client type.

**Force Chest GUI for Everyone:**
```yaml
bedrock:
  enabled: true
  forceChestGui: true
```
All players use chest GUIs for a consistent experience.

## How It Works

When a player executes a home command (`/home`, `/sethome`, `/delhome`):

1. JExHome checks if Floodgate is available
2. If available, it queries Floodgate to determine if the player is using Bedrock Edition
3. Based on the result and configuration, it displays either:
   - **Bedrock Forms** - Native UI elements (SimpleForm, CustomForm, ModalForm)
   - **Chest GUI** - Traditional inventory-based interface

### Bedrock Forms

| Action | Form Type | Description |
|--------|-----------|-------------|
| View Homes | SimpleForm | List of homes with teleport buttons |
| Create Home | CustomForm | Text input for home name |
| Delete Home | ModalForm | Confirmation dialog |

## Graceful Degradation

JExHome handles missing dependencies gracefully:

- **Floodgate not installed**: All players use chest GUI, info message logged at startup
- **Geyser not installed**: Bedrock players cannot connect (not JExHome's responsibility)
- **Form send fails**: Falls back to chat messages

## Multi-Language Support

Bedrock forms support multiple languages through the translation system. Form titles, content, and button labels are loaded from the translation files:

- `translations/en_US.yml` - English translations
- `translations/de_DE.yml` - German translations

Add your own translations by creating additional locale files with the `bedrock.*` keys.

## Using GeyserService in Other Plugins

Other plugins in the ecosystem can use the `GeyserService` from RPlatform:

```java
// In your plugin initialization
platform.initializeGeyser();

// Later, check if a player is on Bedrock
GeyserService geyserService = platform.getGeyserService();
if (geyserService != null && geyserService.isBedrockPlayer(player)) {
    // Show Bedrock-specific UI
}
```

## Troubleshooting

### Bedrock players see chest GUI instead of forms

1. Verify Floodgate is installed and enabled
2. Check that `bedrock.enabled` is `true` in config
3. Check that `bedrock.forceChestGui` is `false`
4. Look for "Floodgate detected" message in server startup logs

### "Floodgate not detected" message at startup

This is normal if you don't have Floodgate installed. JExHome will work fine for Java players.

### Forms not appearing for Bedrock players

1. Ensure Geyser and Floodgate versions are compatible
2. Check Floodgate is properly linked with Geyser
3. Verify the player is actually connecting via Bedrock (check Floodgate prefix)
