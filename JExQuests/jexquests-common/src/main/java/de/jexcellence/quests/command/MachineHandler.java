package de.jexcellence.quests.command;

import com.raindropcentral.commands.v2.CommandContext;
import com.raindropcentral.commands.v2.CommandHandler;
import de.jexcellence.jextranslate.R18nManager;
import de.jexcellence.quests.JExQuests;
import de.jexcellence.quests.machine.MachineDefinitionLoader;
import de.jexcellence.quests.machine.MachineItem;
import de.jexcellence.quests.machine.MachineRegistry;
import de.jexcellence.quests.machine.MachineType;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

/** JExCommand 2.0 handlers for {@code /machine}. */
public final class MachineHandler {

    private final JExQuests quests;
    private final MachineRegistry registry;

    public MachineHandler(@NotNull JExQuests quests) {
        this.quests = quests;
        this.registry = quests.machineRegistry();
    }

    public @NotNull Map<String, CommandHandler> handlerMap() {
        return Map.ofEntries(
                Map.entry("machine.list", this::onList),
                Map.entry("machine.info", this::onInfo),
                Map.entry("machine.give", this::onGive),
                Map.entry("machine.reload", this::onReload)
        );
    }

    private void onList(@NotNull CommandContext ctx) {
        if (this.registry.size() == 0) {
            r18n().msg("machine.list-empty").prefix().send(ctx.sender());
            return;
        }
        ctx.asPlayer().ifPresentOrElse(
                player -> this.quests.viewFrame().open(
                        de.jexcellence.quests.view.MachineTypeOverviewView.class,
                        player,
                        Map.of("plugin", this.quests)
                ),
                () -> this.registry.all().stream()
                        .sorted(Comparator.comparing(MachineType::identifier))
                        .forEach(type ->
                                r18n().msg("machine.list-entry").prefix()
                                        .with("type", type.identifier())
                                        .with("description", type.description().isEmpty() ? type.displayName() : type.description())
                                        .send(ctx.sender()))
        );
    }

    private void onInfo(@NotNull CommandContext ctx) {
        final String typeId = ctx.require("type", String.class);
        final Optional<MachineType> type = this.registry.get(typeId);
        if (type.isEmpty()) {
            r18n().msg("machine.not-found").prefix().with("type", typeId).send(ctx.sender());
            return;
        }
        r18n().msg("machine.list-entry").prefix()
                .with("type", type.get().identifier())
                .with("description", type.get().displayName())
                .send(ctx.sender());
    }

    private void onGive(@NotNull CommandContext ctx) {
        final OfflinePlayer target = ctx.require("player", OfflinePlayer.class);
        final String typeId = ctx.require("type", String.class);
        final Optional<MachineType> resolved = this.registry.get(typeId);
        if (resolved.isEmpty()) {
            r18n().msg("machine.not-found").prefix().with("type", typeId).send(ctx.sender());
            return;
        }

        final Player online = target.getPlayer();
        if (online == null) {
            r18n().msg("error.player-not-found").prefix()
                    .with("player", target.getName() != null ? target.getName() : "?")
                    .send(ctx.sender());
            return;
        }

        final ItemStack stack = MachineItem.createFor(this.quests.getPlugin(), resolved.get());
        final var overflow = online.getInventory().addItem(stack);
        if (!overflow.isEmpty()) {
            overflow.values().forEach(extra ->
                    online.getWorld().dropItemNaturally(online.getLocation(), extra));
            r18n().msg("machine.give.inventory-full").prefix().send(ctx.sender());
        }
        r18n().msg("machine.give.success").prefix()
                .with("type", typeId)
                .with("player", online.getName())
                .send(ctx.sender());
    }

    private void onReload(@NotNull CommandContext ctx) {
        final int count = new MachineDefinitionLoader(
                this.quests.getPlugin(), this.registry, this.quests.logger()
        ).load();
        r18n().msg("machine.reloaded").prefix()
                .with("count", String.valueOf(count))
                .send(ctx.sender());
    }

    private static R18nManager r18n() { return R18nManager.getInstance(); }
}
