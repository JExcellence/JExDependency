package de.jexcellence.quests.command;

import com.raindropcentral.commands.v2.CommandMessages;
import de.jexcellence.jextranslate.R18nManager;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * {@link CommandMessages} implementation that routes every JExCommand
 * 2.0 framework key and every plugin-specific key through R18nManager.
 * Built-in keys ({@code jexcommand.error.*}, {@code jexcommand.usage})
 * and plugin keys ({@code quest.*}, {@code rank.*}, etc.) share the
 * same dispatch — translations live in {@code translations/en_US.yml}.
 */
public final class R18nCommandMessages implements CommandMessages {

    @Override
    public void send(
            @NotNull CommandSender sender,
            @NotNull String key,
            @NotNull Map<String, String> placeholders
    ) {
        var builder = R18nManager.getInstance().msg(key).prefix();
        for (final var entry : placeholders.entrySet()) {
            builder = builder.with(entry.getKey(), entry.getValue());
        }
        builder.send(sender);
    }
}
