package de.jexcellence.economy.command;

import com.raindropcentral.commands.v2.CommandMessages;
import de.jexcellence.jextranslate.R18nManager;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * {@link CommandMessages} implementation that routes every key through the
 * plugin's R18n translator, automatically prefixing all messages.
 *
 * <p>Built-in JExCommand error keys ({@code jexcommand.error.*},
 * {@code jexcommand.usage}) and plugin-specific keys (e.g.
 * {@code economy.error.currency-not-found}) share the same dispatch path —
 * translations live in {@code translations/en_US.yml}.
 *
 * <p>This class is stateless; a single instance can be shared across every
 * registered command tree.
 *
 * @author JExcellence
 * @since 3.0.0
 */
public final class R18nCommandMessages implements CommandMessages {

    @Override
    public void send(@NotNull CommandSender sender,
                     @NotNull String key,
                     @NotNull Map<String, String> placeholders) {
        var builder = R18nManager.getInstance().msg(key).prefix();
        for (var entry : placeholders.entrySet()) {
            builder = builder.with(entry.getKey(), entry.getValue());
        }
        builder.send(sender);
    }
}
