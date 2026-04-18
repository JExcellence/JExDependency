package com.raindropcentral.commands.v2;

import com.raindropcentral.commands.v2.argument.ArgumentType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Runtime {@link Command} implementation that dispatches JExCommand 2.0 command trees.
 *
 * <p>On {@code execute}:
 * <ol>
 *   <li>Walks the tree, matching leading tokens against subcommand names/aliases.</li>
 *   <li>Validates the sender kind at every visited node.</li>
 *   <li>Validates the permission at every visited node (parents first).</li>
 *   <li>Parses the remaining tokens against the node's {@code argumentSchema}.</li>
 *   <li>Invokes the {@link CommandHandler} registered for the node's path.</li>
 * </ol>
 *
 * <p>Errors at each step are surfaced through the supplied {@link CommandMessages}
 * SPI using the keys documented on that interface.
 *
 * <p>On {@code tabComplete}:
 * <ul>
 *   <li>If the current token is at a subcommand position, emits subcommand names
 *       (filtered by permission and sender type) plus the node's own first
 *       argument completions if it is also a leaf handler.</li>
 *   <li>Otherwise delegates to the matched argument's {@link ArgumentType#complete}.</li>
 * </ul>
 *
 * @author JExcellence
 * @since 2.0.0
 */
public final class CommandTreeHandler extends Command {

    private final CommandDefinition root;
    private final Map<String, CommandHandler> handlers;
    private final CommandMessages messages;

    public CommandTreeHandler(@NotNull CommandDefinition root,
                              @NotNull Map<String, CommandHandler> handlers,
                              @NotNull CommandMessages messages) {
        super(
                root.name(),
                root.description() == null ? "" : root.description(),
                "/" + root.name(),
                root.aliases()
        );
        this.root = root;
        this.handlers = Map.copyOf(handlers);
        this.messages = messages;
        if (root.hasPermission()) {
            setPermission(root.permission());
        }
    }

    /** The compiled tree this handler dispatches. */
    public @NotNull CommandDefinition definition() {
        return root;
    }

    // ── Execution ────────────────────────────────────────────────────────────

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        var walk = walk(sender, args, true);
        if (walk.halted()) return true;

        var node = walk.node();
        var handler = handlers.get(node.path());
        if (handler == null) {
            // No registered handler at this path — emit usage hint.
            messages.send(sender, "jexcommand.usage", Map.of("usage", node.usage(root.name())));
            return true;
        }

        var parsed = new HashMap<String, Object>();
        var ok = parseArguments(sender, node, walk.remainingArgs(), parsed);
        if (!ok) return true;

        // Reject extraneous tokens beyond the schema if no subcommand chain handles them.
        if (walk.remainingArgs().length > node.arguments().size()) {
            var excess = walk.remainingArgs()[node.arguments().size()];
            messages.send(sender, "jexcommand.error.too-many-arguments",
                    Map.of("value", excess));
            messages.send(sender, "jexcommand.usage",
                    Map.of("usage", node.usage(root.name())));
            return true;
        }

        try {
            handler.handle(new CommandContext(sender, alias, node.path(), parsed, walk.remainingArgs()));
        } catch (RuntimeException e) {
            messages.send(sender, "jexcommand.error.internal", Map.of("message",
                    e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
        }
        return true;
    }

    // ── Tab completion ───────────────────────────────────────────────────────

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender,
                                              @NotNull String alias,
                                              @NotNull String[] args) {
        if (args.length == 0) return List.of();

        // Walk greedily without halting on errors — completion must be tolerant.
        var node = root;
        var consumed = 0;
        while (consumed < args.length - 1) {
            var token = args[consumed];
            var sub = node.findSubcommand(token).orElse(null);
            if (sub == null) break;
            node = sub;
            consumed++;
        }

        var current = args[args.length - 1].toLowerCase(Locale.ROOT);
        var result = new ArrayList<String>();

        // Subcommand suggestions — only when the cursor is at the subcommand slot.
        if (consumed == args.length - 1) {
            for (var sub : node.subcommands()) {
                if (!senderAllowed(sender, sub)) continue;
                if (sub.hasPermission() && !sender.hasPermission(sub.permission())) continue;
                if (sub.name().toLowerCase(Locale.ROOT).startsWith(current)) {
                    result.add(sub.name());
                }
            }
        }

        // Argument suggestions — how many args have we consumed past the subcommand?
        int argIndex = (args.length - 1) - consumed;
        if (argIndex >= 0 && argIndex < node.arguments().size()) {
            var spec = node.arguments().get(argIndex);
            result.addAll(spec.type().complete(sender, args[args.length - 1]));
        }

        return result;
    }

    // ── Walk helper ──────────────────────────────────────────────────────────

    private @NotNull Walk walk(@NotNull CommandSender sender, @NotNull String[] args, boolean emitErrors) {
        var node = root;

        // Root permission + sender checks always apply.
        if (!senderAllowed(sender, node)) {
            if (emitErrors) sendSenderError(sender, node);
            return Walk.abort();
        }
        if (node.hasPermission() && !sender.hasPermission(node.permission())) {
            if (emitErrors) messages.send(sender, "jexcommand.error.no-permission",
                    Map.of("permission", node.permission()));
            return Walk.abort();
        }

        var consumed = 0;
        while (consumed < args.length) {
            var token = args[consumed];
            var sub = node.findSubcommand(token).orElse(null);
            if (sub == null) break;
            if (!senderAllowed(sender, sub)) {
                if (emitErrors) sendSenderError(sender, sub);
                return Walk.abort();
            }
            if (sub.hasPermission() && !sender.hasPermission(sub.permission())) {
                if (emitErrors) messages.send(sender, "jexcommand.error.no-permission",
                        Map.of("permission", sub.permission()));
                return Walk.abort();
            }
            node = sub;
            consumed++;
        }

        var remaining = new String[args.length - consumed];
        System.arraycopy(args, consumed, remaining, 0, remaining.length);
        return new Walk(node, remaining, false);
    }

    private boolean senderAllowed(@NotNull CommandSender sender, @NotNull CommandDefinition node) {
        return (sender instanceof Player) ? node.allowsPlayer() : node.allowsConsole();
    }

    private void sendSenderError(@NotNull CommandSender sender, @NotNull CommandDefinition node) {
        messages.send(sender, (sender instanceof Player)
                ? "jexcommand.error.not-a-console"
                : "jexcommand.error.not-a-player");
    }

    // ── Argument parsing ─────────────────────────────────────────────────────

    private boolean parseArguments(@NotNull CommandSender sender,
                                    @NotNull CommandDefinition node,
                                    @NotNull String[] tokens,
                                    @NotNull Map<String, Object> out) {
        var specs = node.arguments();
        for (int i = 0; i < specs.size(); i++) {
            var spec = specs.get(i);
            String raw;
            if (i < tokens.length) {
                raw = tokens[i];
            } else if (spec.required()) {
                messages.send(sender, "jexcommand.error.missing-argument",
                        Map.of("name", spec.name()));
                messages.send(sender, "jexcommand.usage",
                        Map.of("usage", node.usage(root.name())));
                return false;
            } else {
                if (spec.defaultValue() != null) {
                    raw = spec.defaultValue();
                } else {
                    // Optional with no default — skip; handler sees null.
                    continue;
                }
            }

            var type = spec.type();
            var result = parseTyped(sender, type, raw);
            if (result.isErr()) {
                messages.send(sender, result.errorKey(), result.placeholders());
                messages.send(sender, "jexcommand.usage",
                        Map.of("usage", node.usage(root.name())));
                return false;
            }
            out.put(spec.name(), result.value());
        }
        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentType.ParseResult<?> parseTyped(CommandSender sender, ArgumentType<?> type, String raw) {
        return ((ArgumentType) type).parse(sender, raw);
    }

    // ── Walk result ──────────────────────────────────────────────────────────

    private record Walk(CommandDefinition node, String[] remainingArgs, boolean halted) {
        static Walk abort() { return new Walk(null, new String[0], true); }
    }
}
