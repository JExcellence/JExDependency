# JExCommand

Annotation-driven command discovery for Paper and Spigot plugins. JExCommand scans the plugin classpath, instantiates annotated handlers, and registers them with Bukkit while keeping configuration, permissions, and localization consistent across editions.

## Core concepts

- **`@Command` annotation** – Marks classes that should be discovered. Each handler must provide a constructor that accepts its section object followed by any service dependencies.
- **Section pairing** – For a handler named `SpeedCommand`, provide a `SpeedCommandSection` extending the shared base section. Sections expose validated configuration and are created before the handler instance.
- **Configuration files** – Defaults ship under `resources/commands/<Command>.yml`. The factory binds YAML nodes to the section class via Jakarta Validation annotations.
- **Permission evaluation** – Utilities in `PermissionsSection` provide consistent `hasPermission` checks and error generation. Throw `CommandError` when senders violate constraints.

## Lifecycle

1. **Discovery** – `CommandFactory` inspects the plugin class loader for `@Command`-annotated types inside the configured package roots.
2. **Section creation** – The matching `*Section` class is instantiated and validated.
3. **Handler wiring** – The section plus plugin/context dependencies are injected into the handler constructor.
4. **Registration** – Commands and listeners are registered with Paper/Bukkit along with tab completers, aliases, and translated error messages.

## Example

```java
@Command(name = "speed", permission = "rdc.command.speed")
public final class SpeedCommand {
    private final SpeedCommandSection section;
    private final TranslationService translation;

    public SpeedCommand(SpeedCommandSection section, TranslationService translation) {
        this.section = section;
        this.translation = translation;
    }

    public void execute(CommandContext context) {
        Player player = context.requirePlayer();
        double multiplier = section.getMultiplier();
        // business logic
        translation.send(player, "command.speed.applied", Placeholder.parsed("multiplier", multiplier));
    }
}
```

## Logging and testing

- Commands surface operational telemetry through `CentralLogger`; use command-specific logger names (e.g., `rdc.command.speed`) so administrators can filter per feature.
- Register integration tests with MockBukkit where possible. Instantiate the section with synthetic YAML and verify permission gating plus translated feedback.

## Related classes

- [`CommandFactory`](src/main/java/com/raindropcentral/commands/CommandFactory.java) – Discovery and registration pipeline.
- [`CommandSection`](src/main/java/com/raindropcentral/commands/section/CommandSection.java) – Base class for command configuration.
- [`CommandError`](src/main/java/com/raindropcentral/commands/error/CommandError.java) – Standardised exception for user-facing failures.
