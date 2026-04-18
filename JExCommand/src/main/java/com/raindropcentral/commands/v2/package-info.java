/**
 * JExCommand 2.0 — declarative YAML-driven command trees.
 *
 * <p>This package provides an alternative registration model where a single YAML
 * document describes an entire command tree (root + arbitrarily nested
 * subcommands) and plugins supply handler callbacks keyed by dot-separated paths.
 * The 1.x API under
 * {@link com.raindropcentral.commands.BukkitCommand BukkitCommand} remains fully
 * supported and unchanged.
 *
 * <h2>Core types</h2>
 * <ul>
 *   <li>{@link com.raindropcentral.commands.v2.CommandSectionV2} — YAML-mirroring
 *       declarative record.</li>
 *   <li>{@link com.raindropcentral.commands.v2.CommandDefinition} — compiled, type-resolved tree.</li>
 *   <li>{@link com.raindropcentral.commands.v2.CommandTreeLoader} — YAML → CommandDefinition.</li>
 *   <li>{@link com.raindropcentral.commands.v2.CommandTreeHandler} — Bukkit {@code Command}
 *       that dispatches a tree at runtime.</li>
 *   <li>{@link com.raindropcentral.commands.v2.CommandHandler} — functional interface executed
 *       at matched paths.</li>
 *   <li>{@link com.raindropcentral.commands.v2.CommandContext} — typed argument bundle handed
 *       to handlers.</li>
 *   <li>{@link com.raindropcentral.commands.v2.CommandMessages} — i18n SPI; plugins supply their
 *       own translator.</li>
 * </ul>
 *
 * <h2>Typical entry point</h2>
 *
 * <pre>
 * var registry = ArgumentTypeRegistry.defaults();
 * var factory = new CommandFactory(plugin);
 *
 * factory.registerTree(
 *     "commands/economy.yml",
 *     Map.of(
 *         "economy",       ctx -&gt; sendHelp(ctx),
 *         "economy.give",  ctx -&gt; give(ctx),
 *         "economy.take",  ctx -&gt; take(ctx)),
 *     new MyCommandMessages(translator),
 *     registry);
 * </pre>
 *
 * @author JExcellence
 * @since 2.0.0
 */
package com.raindropcentral.commands.v2;
