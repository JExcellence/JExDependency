package de.jexcellence.quests.command;

import com.raindropcentral.commands.v2.CommandContext;
import com.raindropcentral.commands.v2.CommandHandler;
import de.jexcellence.core.stats.StatisticsDelivery;
import de.jexcellence.jextranslate.R18nManager;
import de.jexcellence.quests.JExQuests;
import de.jexcellence.quests.migration.PlayerStateExporter;
import de.jexcellence.quests.migration.PlayerStateImporter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * JExCommand 2.0 handlers for {@code /jexquests} — the admin surface.
 * Covers live info, subsystem status, and hot-reload of every YAML
 * content loader without restarting the server.
 */
public final class AdminHandler {

    private final JExQuests quests;

    public AdminHandler(@NotNull JExQuests quests) {
        this.quests = quests;
    }

    /**
     * Returns the command handler map for admin commands.
     */
    public @NotNull Map<String, CommandHandler> handlerMap() {
        return Map.ofEntries(
                Map.entry("jexquests", this::onInfo),
                Map.entry("jexquests.info", this::onInfo),
                Map.entry("jexquests.status", this::onStatus),
                Map.entry("jexquests.reload", this::onReload),
                Map.entry("jexquests.export", this::onExport),
                Map.entry("jexquests.export-all", this::onExportAll),
                Map.entry("jexquests.import", this::onImport),
                Map.entry("jexquests.import.dry-run", this::onImportDryRun),
                Map.entry("jexquests.import-all", this::onImportAll)
        );
    }

    private void onImportAll(@NotNull CommandContext ctx) {
        final String folderArg = ctx.require("folder", String.class);
        final Path folder = resolveImportPath(folderArg);
        r18n().msg("jexquests.import-all.starting").prefix()
                .with("path", folder.toString()).send(ctx.sender());
        new PlayerStateImporter(this.quests).importAllAsync(folder)
                .thenAccept(result -> {
                    r18n().msg("jexquests.import-all.summary").prefix()
                            .with("success", String.valueOf(result.successCount()))
                            .with("failed", String.valueOf(result.failureCount()))
                            .with("total", String.valueOf(result.totalCount()))
                            .with("path", result.folder().toString())
                            .send(ctx.sender());
                    result.failures().stream().limit(10).forEach(line ->
                            r18n().msg("jexquests.import-all.failure").prefix()
                                    .with("line", line).send(ctx.sender()));
                    if (result.failures().size() > 10) {
                        r18n().msg("jexquests.import-all.truncated").prefix()
                                .with("remaining", String.valueOf(result.failures().size() - 10))
                                .send(ctx.sender());
                    }
                })
                .exceptionally(ex -> {
                    r18n().msg("jexquests.import-all.failed").prefix()
                            .with("error", ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName())
                            .send(ctx.sender());
                    return null;
                });
    }

    private void onExportAll(@NotNull CommandContext ctx) {
        r18n().msg("jexquests.export-all.starting").prefix().send(ctx.sender());
        new PlayerStateExporter(this.quests).exportAllAsync()
                .thenAccept(result -> {
                    r18n().msg("jexquests.export-all.summary").prefix()
                            .with("success", String.valueOf(result.successCount()))
                            .with("failed", String.valueOf(result.failureCount()))
                            .with("total", String.valueOf(result.totalCount()))
                            .with("path", result.outputDirectory().toString())
                            .send(ctx.sender());
                    result.failures().stream().limit(10).forEach(line ->
                            r18n().msg("jexquests.export-all.failure").prefix()
                                    .with("line", line).send(ctx.sender()));
                    if (result.failures().size() > 10) {
                        r18n().msg("jexquests.export-all.truncated").prefix()
                                .with("remaining", String.valueOf(result.failures().size() - 10))
                                .send(ctx.sender());
                    }
                })
                .exceptionally(ex -> {
                    r18n().msg("jexquests.export-all.failed").prefix()
                            .with("error", ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName())
                            .send(ctx.sender());
                    return null;
                });
    }

    private void onExport(@NotNull CommandContext ctx) {
        final OfflinePlayer target = ctx.require("player", OfflinePlayer.class);
        r18n().msg("jexquests.export.starting").prefix()
                .with("player", target.getName() != null ? target.getName() : target.getUniqueId().toString())
                .send(ctx.sender());
        new PlayerStateExporter(this.quests).exportAsync(target.getUniqueId())
                .thenAccept(path -> r18n().msg("jexquests.export.success").prefix()
                        .with("player", target.getName() != null ? target.getName() : target.getUniqueId().toString())
                        .with("path", path.toString())
                        .send(ctx.sender()))
                .exceptionally(ex -> {
                    r18n().msg("jexquests.export.failed").prefix()
                            .with("error", ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName())
                            .send(ctx.sender());
                    return null;
                });
    }

    private void onImport(@NotNull CommandContext ctx) {
        final String file = ctx.require("file", String.class);
        final Path source = resolveImportPath(file);
        r18n().msg("jexquests.import.starting").prefix()
                .with("path", source.toString())
                .send(ctx.sender());
        new PlayerStateImporter(this.quests).importAsync(source)
                .thenAccept(uuid -> r18n().msg("jexquests.import.success").prefix()
                        .with("uuid", uuid.toString())
                        .with("path", source.toString())
                        .send(ctx.sender()))
                .exceptionally(ex -> {
                    r18n().msg("jexquests.import.failed").prefix()
                            .with("error", ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName())
                            .send(ctx.sender());
                    return null;
                });
    }

    private void onImportDryRun(@NotNull CommandContext ctx) {
        final String file = ctx.require("file", String.class);
        final Path source = resolveImportPath(file);
        r18n().msg("jexquests.import.dry-run.starting").prefix()
                .with("path", source.toString())
                .send(ctx.sender());
        new PlayerStateImporter(this.quests).dryRunAsync(source)
                .thenAccept(preview -> {
                    r18n().msg("jexquests.import.dry-run.summary").prefix()
                            .with("player", preview.playerName())
                            .with("uuid", preview.playerUuid().toString())
                            .with("created", String.valueOf(preview.createdCount()))
                            .with("updated", String.valueOf(preview.updatedCount()))
                            .with("unchanged", String.valueOf(preview.unchangedCount()))
                            .send(ctx.sender());
                    if (preview.isNoOp()) {
                        r18n().msg("jexquests.import.dry-run.noop").prefix().send(ctx.sender());
                        return;
                    }
                    preview.changes().stream().limit(20).forEach(change ->
                            r18n().msg("jexquests.import.dry-run.change").prefix()
                                    .with("change", change)
                                    .send(ctx.sender()));
                    if (preview.changes().size() > 20) {
                        r18n().msg("jexquests.import.dry-run.truncated").prefix()
                                .with("remaining", String.valueOf(preview.changes().size() - 20))
                                .send(ctx.sender());
                    }
                })
                .exceptionally(ex -> {
                    r18n().msg("jexquests.import.failed").prefix()
                            .with("error", ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName())
                            .send(ctx.sender());
                    return null;
                });
    }

    /**
     * Resolves the user-supplied file argument — absolute paths are
     * used verbatim, relative paths are anchored under the plugin's
     * {@code exports/} directory so admins can round-trip exports
     * without leaving the data folder.
     */
    private @NotNull Path resolveImportPath(@NotNull String raw) {
        final Path candidate = Paths.get(raw);
        if (candidate.isAbsolute()) return candidate;
        return this.quests.getPlugin().getDataFolder().toPath().resolve("exports").resolve(raw);
    }

    private void onInfo(@NotNull CommandContext ctx) {
        r18n().msg("jexquests.header").prefix().send(ctx.sender());
        r18n().msg("jexquests.info.line").prefix()
                .with("edition", this.quests.edition())
                .with("version", this.quests.version())
                .send(ctx.sender());
    }

    private void onStatus(@NotNull CommandContext ctx) {
        final boolean statsBridge = Bukkit.getServicesManager().load(StatisticsDelivery.class) != null;
        r18n().msg("jexquests.status.line").prefix()
                .with("quests", onOff(true))
                .with("ranks", onOff(true))
                .with("bounties", onOff(true))
                .with("perks", onOff(true))
                .with("machines", onOff(this.quests.machineRegistry().size() > 0))
                .send(ctx.sender());
        r18n().msg("jexquests.info.line").prefix()
                .with("edition", "telemetry")
                .with("version", onOff(statsBridge))
                .send(ctx.sender());
    }

    private void onReload(@NotNull CommandContext ctx) {
        final long start = System.nanoTime();
        r18n().msg("jexquests.reload.starting").prefix().send(ctx.sender());
        try {
            this.quests.reloadContent();
            final long ms = (System.nanoTime() - start) / 1_000_000L;
            r18n().msg("jexquests.reload.success").prefix()
                    .with("duration", String.valueOf(ms))
                    .send(ctx.sender());
        } catch (final RuntimeException ex) {
            this.quests.logger().error("reload failed: {}", ex.getMessage());
            r18n().msg("jexquests.reload.failed").prefix()
                    .with("error", ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName())
                    .send(ctx.sender());
        }
    }

    private static String onOff(boolean value) {
        return value
                ? "<gradient:#86efac:#16a34a>on</gradient>"
                : "<gradient:#fca5a5:#dc2626>off</gradient>";
    }

    private static R18nManager r18n() { return R18nManager.getInstance(); }
}
