package de.jexcellence.core.command;

import com.raindropcentral.commands.v2.CommandMessages;
import de.jexcellence.jextranslate.R18nManager;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * {@link CommandMessages} implementation routing every key through
 * {@link R18nManager}. Built-in JExCommand error keys and plugin-specific
 * keys share the same dispatch path.
 */
public final class R18nCommandMessages implements CommandMessages {

    @Override
    public void send(@NotNull CommandSender sender, @NotNull String key, @NotNull Map<String, String> placeholders) {
        var builder = R18nManager.getInstance().msg(key).prefix();
        for (final Map.Entry<String, String> entry : placeholders.entrySet()) {
            builder = builder.with(entry.getKey(), entry.getValue());
        }
        builder.send(sender);
    }
}
