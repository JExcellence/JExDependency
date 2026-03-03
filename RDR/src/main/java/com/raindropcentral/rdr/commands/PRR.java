/*
 * PRR.java
 *
 * @author RaindropCentral
 * @version 5.0.0
 */

package com.raindropcentral.rdr.commands;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.raindropcentral.commands.PlayerCommand;
import com.raindropcentral.commands.utility.Command;
import com.raindropcentral.rdr.RDR;
import com.raindropcentral.rdr.database.entity.RStorage;
import com.raindropcentral.rdr.view.StorageOverviewView;
import com.raindropcentral.rdr.view.StorageViewLauncher;
import de.jexcellence.evaluable.section.ACommandSection;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

/**
 * Primary player command for the RDR storage plugin.
 *
 * <p>The command exposes the storage overview entry point, quick hotkey opens via
 * {@code /rr <hotkey>}, and first-argument tab completion for supported actions and bound hotkeys.</p>
 *
 * @author RaindropCentral
 * @since 5.0.0
 * @version 5.0.0
 */
@Command
@SuppressWarnings("unused")
public class PRR extends PlayerCommand {

    private final RDR rdr;

    /**
     * Creates the player command handler.
     *
     * @param commandSection configured command section for {@code /prr}
     * @param rdr active plugin instance
     */
    public PRR(final ACommandSection commandSection, final RDR rdr) {
        super(commandSection);
        this.rdr = rdr;
    }

    /**
     * Executes the resolved player action.
     *
     * @param player invoking player
     * @param alias command alias used by the player
     * @param args raw command arguments
     */
    @Override
    protected void onPlayerInvocation(@NotNull Player player, @NotNull String alias, @NonNull @NotNull String[] args) {
        final String firstArgument = args.length == 0 ? null : args[0];
        if (isNumericArgument(firstArgument)) {
            final Integer hotkey = parseHotkeyArgument(firstArgument);
            if (hotkey == null) {
                this.sendInvalidHotkeyMessage(player, this.resolveMaxHotkeys());
                return;
            }
            this.openStorageHotkey(player, hotkey);
            return;
        }

        final EPRRAction action = this.resolveAction(args);
        switch (action) {
            case STORAGE -> this.openStorageOverview(player);
            default -> {
                if (this.hasNoPermission(player, EPRRPermission.INFO)) {
                    return;
                }
                this.openStorageOverview(player);
            }
        }
    }

    /**
     * Returns first-argument action suggestions for the player.
     *
     * @param player player requesting tab completion
     * @param alias command alias entered by the player
     * @param args current argument input
     * @return matching action suggestions, or an empty list when no suggestion applies
     */
    @Override
    protected List<String> onPlayerTabCompletion(@NotNull Player player, @NotNull String alias, @NonNull @NotNull String[] args) {
        if (!this.hasPermission(player, EPRRPermission.COMMAND)) {
            return List.of();
        }
        if (args.length == 1) {
            final List<String> candidates = new ArrayList<>(this.getAvailableActions(player));
            candidates.addAll(this.getAvailableHotkeys(player));
            return StringUtil.copyPartialMatches(args[0], candidates, new ArrayList<>());
        }
        return List.of();
    }

    static boolean isNumericArgument(final @Nullable String rawArgument) {
        if (rawArgument == null) {
            return false;
        }

        final String normalizedArgument = rawArgument.trim();
        if (normalizedArgument.isEmpty()) {
            return false;
        }

        for (int index = 0; index < normalizedArgument.length(); index++) {
            if (!Character.isDigit(normalizedArgument.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    static @Nullable Integer parseHotkeyArgument(final @Nullable String rawArgument) {
        if (!isNumericArgument(rawArgument)) {
            return null;
        }

        try {
            return Integer.parseInt(rawArgument.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private @NotNull EPRRAction resolveAction(
        final @NotNull String[] args
    ) {
        if (args.length == 0 || args[0] == null || args[0].isBlank()) {
            return EPRRAction.INFO;
        }

        try {
            return EPRRAction.valueOf(args[0].trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return EPRRAction.INFO;
        }
    }

    private void openStorageOverview(final @NotNull Player player) {
        if (this.hasNoPermission(player, EPRRPermission.STORAGE) || this.rdr == null) {
            return;
        }

        this.rdr.getViewFrame().open(
            StorageOverviewView.class,
            player,
            Map.of("plugin", this.rdr)
        );
    }

    private @NotNull List<String> getAvailableActions(final @NotNull Player player) {
        final List<String> actions = new ArrayList<>();
        if (this.hasPermission(player, EPRRPermission.STORAGE)) {
            actions.add(EPRRAction.STORAGE.name().toLowerCase(Locale.ROOT));
        }
        return List.copyOf(actions);
    }

    private @NotNull List<String> getAvailableHotkeys(final @NotNull Player player) {
        if (this.rdr == null || !this.hasPermission(player, EPRRPermission.STORAGE) || this.rdr.getStorageRepository() == null) {
            return List.of();
        }

        return this.rdr.getStorageRepository().findAssignedHotkeys(player.getUniqueId()).stream()
            .sorted(Comparator.naturalOrder())
            .map(String::valueOf)
            .toList();
    }

    private void openStorageHotkey(
        final @NotNull Player player,
        final int hotkey
    ) {
        if (this.hasNoPermission(player, EPRRPermission.STORAGE) || this.rdr == null) {
            return;
        }

        final int maxHotkeys = this.resolveMaxHotkeys();
        if (hotkey < 1 || hotkey > maxHotkeys) {
            this.sendInvalidHotkeyMessage(player, maxHotkeys);
            return;
        }

        if (this.rdr.getStorageRepository() == null) {
            this.sendMissingHotkeyMessage(player, hotkey);
            return;
        }

        final RStorage storage = this.rdr.getStorageRepository().findByPlayerAndHotkey(player.getUniqueId(), hotkey);
        if (storage == null) {
            this.sendMissingHotkeyMessage(player, hotkey);
            return;
        }

        StorageViewLauncher.openStorage(player, this.rdr, storage);
    }

    private void sendMissingHotkeyMessage(
        final @NotNull Player player,
        final int hotkey
    ) {
        new I18n.Builder("storage.message.hotkey_missing", player)
            .withPlaceholder("hotkey", hotkey)
            .build()
            .sendMessage();
    }

    private void sendInvalidHotkeyMessage(
        final @NotNull Player player,
        final int maxHotkeys
    ) {
        new I18n.Builder("storage.message.hotkey_invalid", player)
            .withPlaceholder("max_hotkeys", maxHotkeys)
            .build()
            .sendMessage();
    }

    private int resolveMaxHotkeys() {
        return this.rdr == null ? 9 : this.rdr.getDefaultConfig().getMaxHotkeys();
    }
}
