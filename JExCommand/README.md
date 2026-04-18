# JExCommand

Annotation-driven command discovery for Bukkit and Paper plugins. Scans the classpath, wires configuration sections, and registers handlers with zero boilerplate.

---

## Installation

```kotlin
dependencies {
    compileOnly("com.raindropcentral.commands:jexcommand:1.0.1")
}
```

Provided at runtime via JExDependency.

---

## Quick Start

```java
@Override
public void onEnable() {
    new CommandFactory(this).registerAllCommandsAndListeners();
}
```

That single line discovers every `@Command`-annotated handler and every `Listener` in your plugin, pairs them with their configuration sections, and registers them with Bukkit.

---

## Player Commands

Extend `PlayerCommand` for player-only execution. The base class validates the sender, parses arguments, and routes errors through localized messages.

```java
@Command
public final class PSpeed extends PlayerCommand {

    private final PSpeedSection section;

    public PSpeed(PSpeedSection section, MyPlugin plugin) {
        super(section);
        this.section = section;
    }

    @Override
    protected void onPlayerInvocation(Player player, String alias, String[] args) {
        if (hasNoPermission(player, section.getPermissions().getUse())) return;

        var speed = floatParameterOrElse(args, 0, 0.2f);
        player.setWalkSpeed(speed);
        // send feedback via JExTranslate
    }

    @Override
    protected List<String> onPlayerTabCompletion(Player player, String alias, String[] args) {
        return List.of("0.2", "0.5", "1.0");
    }
}
```

Key features:
- `hasNoPermission(player, node)` checks permission and sends a localized denial message when blocked
- `hasPermission(player, node)` checks silently (no message)
- Non-player senders receive a localized "not a player" error automatically

---

## Console Commands

Extend `ServerCommand` for console-only execution.

```java
@Command
public final class CReload extends ServerCommand {

    public CReload(CReloadSection section, MyPlugin plugin) {
        super(section);
    }

    @Override
    protected void onConsoleInvocation(ConsoleCommandSender console, String alias, String[] args) {
        console.sendMessage("Reloading...");
    }
}
```

Player senders receive a localized "not a console" error automatically.

---

## Argument Parsing

Type-safe parsers with localized errors on failure. Every parser has a required and an optional variant:

```java
// Required -- throws CommandError if missing or malformed
var speed  = doubleParameter(args, 0);
var target = playerParameter(args, 1);
var mode   = enumParameter(args, 2, GameMode.class);

// Optional -- returns fallback if absent, still throws if present but malformed
var speed  = doubleParameterOrElse(args, 0, 1.0);
var target = playerParameterOrElse(args, 1, player);
var mode   = enumParameterOrElse(args, 2, GameMode.class, GameMode.SURVIVAL);
```

| Parser | Return | Error type |
|---|---|---|
| `stringParameter` | `String` | `MISSING_ARGUMENT` |
| `integerParameter` | `Integer` | `MALFORMED_INTEGER` |
| `longParameter` | `Long` | `MALFORMED_LONG` |
| `doubleParameter` | `Double` | `MALFORMED_DOUBLE` |
| `floatParameter` | `Float` | `MALFORMED_FLOAT` |
| `uuidParameter` | `UUID` | `MALFORMED_UUID` |
| `enumParameter` | `T extends Enum` | `MALFORMED_ENUM` |
| `playerParameter` | `Player` | `PLAYER_NOT_ONLINE` |
| `offlinePlayerParameter` | `OfflinePlayer` | `PLAYER_UNKNOWN` |

All errors are caught by the base class and translated into localized player-facing messages through the command section.

---

## Section Pairing

Each command `PFoo` expects a companion `PFooSection` extending `ACommandSection` in the same package. The section provides:

- Command name, description, usage, aliases
- Localized error messages for every `CommandError` type
- A `PermissionsSection` with configured permission nodes
- Custom configuration fields

YAML files live at `plugins/<Plugin>/commands/<pfoo>.yml` and are loaded by the factory automatically.

---

## Permission Inheritance

When a section implements `PermissionParentProvider`, the factory registers parent-child relationships with Bukkit so granting a parent implicitly grants its children:

```java
public class PFooSection extends ACommandSection implements PermissionParentProvider {

    @Override
    public Map<String, List<String>> getPermissionParents() {
        return Map.of("admin", List.of("use", "reload"));
    }
}
```

Resolution logic lives in `PermissionNodeResolver`, which maps internal names to their configured permission strings and walks the hierarchy with cycle detection.

---

## Edition Gating

Pass a context object for edition-aware constructor injection:

```java
// Premium edition passes its context
new CommandFactory(this, rdqPremiumInstance).registerAllCommandsAndListeners();
```

Constructor selection precedence:

1. **Exact match** -- parameter type equals the context object's class
2. **Polymorphic** -- context object is assignable to the parameter type
3. **Plugin direct** -- parameter accepts the plugin's concrete class
4. **Plugin superclass** -- parameter accepts any superclass of the plugin

This allows shared command classes across free and premium editions where `RDQFree` and `RDQPremium` both extend `RDQ`.

---

## Listener Registration

Listeners in `listener` or `listeners` packages are discovered and registered automatically. No annotation needed -- the factory checks `Listener.isAssignableFrom()`:

```java
public class PlayerJoinListener implements Listener {

    private final MyPlugin plugin;

    public PlayerJoinListener(MyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // ...
    }
}
```

Same constructor injection precedence as commands.

---

## Architecture

```
com.raindropcentral.commands
    BukkitCommand           Abstract base: argument parsing, error handling
      PlayerCommand         Player-only: permission checks, tab completion
      ServerCommand         Console-only: sender validation
    CommandFactory           Discovery, section wiring, Bukkit registration
    utility/
      @Command              Marker annotation for discovery
    permission/
      PermissionNodeResolver      Shared reflection-based node extraction
      PermissionHierarchyRegistrar Bukkit permission tree registration
      PermissionParentProvider     Interface for declaring hierarchies
```

Dependencies (all `compileOnly`):
- Paper API
- Adventure (Components, legacy serializer)
- JExConfig (Evaluable sections, ConfigMapper, CommandUpdater)
- JExTranslate (i18n message builder)
