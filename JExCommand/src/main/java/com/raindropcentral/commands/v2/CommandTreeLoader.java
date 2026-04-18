package com.raindropcentral.commands.v2;

import com.raindropcentral.commands.v2.argument.ArgumentSpec;
import com.raindropcentral.commands.v2.argument.ArgumentType;
import com.raindropcentral.commands.v2.argument.ArgumentTypeRegistry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads a JExCommand 2.0 YAML command tree and compiles it to a {@link CommandDefinition}.
 *
 * <p>The loader is deliberately YAML-agnostic in its public shape: callers hand it a
 * {@link YamlConfiguration}, an {@link InputStream}, a {@link File}, or a classpath
 * resource path, and receive a validated, type-resolved definition.
 *
 * <p><b>Accepted structure:</b>
 * <pre>
 * name: economy
 * description: "…"
 * aliases: [eco, econ]
 * permission: "economy.command"
 * senders: [console, player]
 * argumentSchema:
 *   - { name: foo, type: string, required: true }
 *   - { name: bar, type: positive_double, required: false, defaultValue: "0" }
 * subcommands:
 *   - name: give
 *     permission: "economy.command.give"
 *     argumentSchema: [ … ]
 * </pre>
 *
 * <p>Unknown argument types (after consulting the supplied registry) and missing
 * required fields cause a {@link LoadException}.
 *
 * @author JExcellence
 * @since 2.0.0
 */
public final class CommandTreeLoader {

    private final ArgumentTypeRegistry registry;

    public CommandTreeLoader(@NotNull ArgumentTypeRegistry registry) {
        this.registry = registry;
    }

    /** Convenience — uses {@link ArgumentTypeRegistry#defaults()}. */
    public CommandTreeLoader() {
        this(ArgumentTypeRegistry.defaults());
    }

    // ── Public entry points ──────────────────────────────────────────────────

    /** Loads a tree from an already-parsed {@link YamlConfiguration}. */
    public @NotNull CommandDefinition load(@NotNull YamlConfiguration yaml) {
        var section = parseSection(yaml, "root");
        return compile(section, section.name());
    }

    /** Loads a tree from a file on disk. */
    public @NotNull CommandDefinition load(@NotNull File file) {
        var yaml = YamlConfiguration.loadConfiguration(file);
        return load(yaml);
    }

    /** Loads a tree from an {@link InputStream} (closed by this method). */
    public @NotNull CommandDefinition load(@NotNull InputStream stream) {
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            var yaml = YamlConfiguration.loadConfiguration(reader);
            return load(yaml);
        } catch (IOException e) {
            throw new LoadException("Failed to read YAML stream: " + e.getMessage(), e);
        }
    }

    /**
     * Loads a tree from a classpath resource.
     *
     * @param loader      class loader used to open the resource
     * @param resourcePath slash-separated resource path
     */
    public @NotNull CommandDefinition loadResource(@NotNull ClassLoader loader,
                                                   @NotNull String resourcePath) {
        try (var in = loader.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new LoadException("Resource not found: " + resourcePath);
            }
            return load(in);
        } catch (IOException e) {
            throw new LoadException("Failed to read resource " + resourcePath + ": " + e.getMessage(), e);
        }
    }

    // ── Parsing ──────────────────────────────────────────────────────────────

    private @NotNull CommandSectionV2 parseSection(@NotNull ConfigurationSection cfg,
                                                    @NotNull String pathHint) {
        var name = requireString(cfg, "name", pathHint);
        var description = cfg.getString("description");
        var permission = cfg.getString("permission");
        var aliases = cfg.getStringList("aliases");
        var senders = cfg.getStringList("senders");

        var args = new ArrayList<ArgumentSpec>();
        if (cfg.isList("argumentSchema")) {
            var raw = cfg.getList("argumentSchema");
            if (raw != null) {
                for (int i = 0; i < raw.size(); i++) {
                    var entry = raw.get(i);
                    if (!(entry instanceof Map<?, ?> map)) {
                        throw new LoadException("argumentSchema[" + i + "] at '" + pathHint
                                + "' must be a map");
                    }
                    args.add(parseArg(map, pathHint + ".argumentSchema[" + i + "]"));
                }
            }
        }

        var subs = new ArrayList<CommandSectionV2>();
        if (cfg.isList("subcommands")) {
            var raw = cfg.getList("subcommands");
            if (raw != null) {
                for (int i = 0; i < raw.size(); i++) {
                    var entry = raw.get(i);
                    if (!(entry instanceof Map<?, ?> map)) {
                        throw new LoadException("subcommands[" + i + "] at '" + pathHint
                                + "' must be a map");
                    }
                    subs.add(parseSection(mapToSection(map), pathHint + "." + name + "[" + i + "]"));
                }
            }
        } else if (cfg.isConfigurationSection("subcommands")) {
            var sub = cfg.getConfigurationSection("subcommands");
            if (sub != null) {
                for (var key : sub.getKeys(false)) {
                    var child = sub.getConfigurationSection(key);
                    if (child == null) continue;
                    // Allow map-style subcommands keyed by name; copy name in if missing.
                    if (!child.isString("name")) child.set("name", key);
                    subs.add(parseSection(child, pathHint + "." + name + "." + key));
                }
            }
        }

        return new CommandSectionV2(
                name,
                description,
                permission,
                aliases,
                senders,
                args,
                subs
        );
    }

    private @NotNull ArgumentSpec parseArg(@NotNull Map<?, ?> map, @NotNull String pathHint) {
        var name = asString(map.get("name"));
        if (name == null || name.isBlank()) {
            throw new LoadException("Missing 'name' at " + pathHint);
        }
        var typeId = asString(map.get("type"));
        if (typeId == null || typeId.isBlank()) {
            throw new LoadException("Missing 'type' at " + pathHint);
        }
        boolean required = true;
        var req = map.get("required");
        if (req instanceof Boolean b) required = b;
        else if (req instanceof String s) required = Boolean.parseBoolean(s);

        var defaultValue = asString(map.get("defaultValue"));
        var description = asString(map.get("description"));
        return new ArgumentSpec(name, typeId, required, defaultValue, description);
    }

    private @NotNull ConfigurationSection mapToSection(@NotNull Map<?, ?> map) {
        var y = new YamlConfiguration();
        for (var e : map.entrySet()) {
            y.set(String.valueOf(e.getKey()), e.getValue());
        }
        return y;
    }

    // ── Compilation ──────────────────────────────────────────────────────────

    private @NotNull CommandDefinition compile(@NotNull CommandSectionV2 section,
                                                @NotNull String path) {
        var resolved = new ArrayList<CommandDefinition.ResolvedArgument>();
        for (var spec : section.argumentSchema()) {
            ArgumentType<?> type = registry.resolve(spec.typeId());
            if (type == null) {
                throw new LoadException("Unknown argument type '" + spec.typeId()
                        + "' at " + path + "/" + spec.name());
            }
            resolved.add(new CommandDefinition.ResolvedArgument(
                    spec.name(), type, spec.required(), spec.defaultValue(), spec.description()));
        }
        var subs = new ArrayList<CommandDefinition>();
        for (var sub : section.subcommands()) {
            subs.add(compile(sub, path + "." + sub.name()));
        }
        return new CommandDefinition(
                section.name(),
                path,
                section.description(),
                section.permission(),
                section.aliases(),
                section.senders(),
                resolved,
                subs
        );
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static @NotNull String requireString(@NotNull ConfigurationSection cfg,
                                                  @NotNull String key,
                                                  @NotNull String pathHint) {
        var value = cfg.getString(key);
        if (value == null || value.isBlank()) {
            throw new LoadException("Missing '" + key + "' at " + pathHint);
        }
        return value;
    }

    private static @Nullable String asString(@Nullable Object value) {
        return value == null ? null : String.valueOf(value);
    }

    // ── Errors ───────────────────────────────────────────────────────────────

    /** Thrown when a tree cannot be parsed or compiled. */
    public static final class LoadException extends RuntimeException {
        public LoadException(@NotNull String message) { super(message); }
        public LoadException(@NotNull String message, @NotNull Throwable cause) { super(message, cause); }
    }

    /**
     * Helper class listing all the positional arguments of a single node, for
     * producing synthetic usage lines when no description is supplied. Kept here
     * so external callers do not need to introspect the tree.
     */
    public static @NotNull List<String> argumentLabels(@NotNull CommandDefinition def) {
        var list = new ArrayList<String>();
        for (var arg : def.arguments()) {
            list.add((arg.required() ? "<" : "[") + arg.name() + (arg.required() ? ">" : "]"));
        }
        return list;
    }
}
