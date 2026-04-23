package de.jexcellence.core.command.core;

import com.raindropcentral.commands.v2.CommandContext;
import com.raindropcentral.commands.v2.CommandHandler;
import de.jexcellence.core.JExCore;
import de.jexcellence.core.database.entity.CorePlayer;
import de.jexcellence.core.service.CorePlayerService;
import de.jexcellence.jextranslate.R18nManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * JExCommand 2.0 handlers for {@code /jexcore}. Covers the root summary
 * message, {@code reload}, {@code info}, and {@code player} subcommands.
 */
public final class CoreHandler {

    private final JExCore core;
    private final CorePlayerService players;

    public CoreHandler(@NotNull JExCore core, @NotNull CorePlayerService players) {
        this.core = core;
        this.players = players;
    }

    public @NotNull Map<String, CommandHandler> handlerMap() {
        return Map.ofEntries(
                Map.entry("jexcore", this::onRoot),
                Map.entry("jexcore.info", this::onInfo),
                Map.entry("jexcore.reload", this::onReload),
                Map.entry("jexcore.player", this::onPlayer)
        );
    }

    private void onRoot(@NotNull CommandContext ctx) {
        onInfo(ctx);
    }

    private void onInfo(@NotNull CommandContext ctx) {
        r18n().msg("jexcore.info.line")
                .prefix()
                .with("edition", this.core.edition())
                .with("version", this.core.version())
                .send(ctx.sender());
    }

    private void onReload(@NotNull CommandContext ctx) {
        try {
            R18nManager.getInstance().reload();
            r18n().msg("jexcore.reload.success").prefix().send(ctx.sender());
        } catch (final RuntimeException ex) {
            r18n().msg("jexcore.reload.failed")
                    .prefix()
                    .with("error", ex.getMessage() != null ? ex.getMessage() : "unknown")
                    .send(ctx.sender());
        }
    }

    private void onPlayer(@NotNull CommandContext ctx) {
        final OfflinePlayer target = ctx.require("target", OfflinePlayer.class);
        final String targetName = target.getName() != null ? target.getName() : target.getUniqueId().toString();

        this.players.findByUuid(target.getUniqueId()).thenAccept(opt -> Bukkit.getScheduler().runTask(this.core.getPlugin(), () -> {
            if (opt.isEmpty()) {
                r18n().msg("jexcore.player.not-tracked")
                        .prefix()
                        .with("player", targetName)
                        .send(ctx.sender());
                return;
            }
            final CorePlayer player = opt.get();
            r18n().msg("jexcore.player.summary")
                    .prefix()
                    .with("player", player.getPlayerName())
                    .with("uuid", player.getUniqueId().toString())
                    .with("first_seen", player.getFirstSeen().toString())
                    .with("last_seen", player.getLastSeen().toString())
                    .send(ctx.sender());
        }));
    }

    private static R18nManager r18n() {
        return R18nManager.getInstance();
    }
}
