package com.raindropcentral.commands;

import de.jexcellence.evaluable.EnumInfo;
import de.jexcellence.evaluable.EnumInfoImpl;
import de.jexcellence.evaluable.error.CommandError;
import de.jexcellence.evaluable.error.ErrorContext;
import de.jexcellence.evaluable.error.ErrorType;
import de.jexcellence.evaluable.section.CommandSection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Supplier;

/**
 * Abstract Bukkit command wrapper bridging execution to custom handlers with
 * type-safe argument parsing, localized error handling, and tab completion.
 *
 * <p>Instances are provisioned by {@link CommandFactory}, which injects the
 * {@link CommandSection} so commands respect edition gating and localization.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 2.0.0
 */
public abstract class BukkitCommand extends Command {

    private static final Logger LOG = LoggerFactory.getLogger(BukkitCommand.class);
    private static final Map<Class<? extends Enum<?>>, EnumInfoImpl> enumConstantsCache =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final LegacyComponentSerializer LEGACY_SERIALIZER =
            LegacyComponentSerializer.legacySection();

    protected final CommandSection commandSection;

    protected BukkitCommand(@NotNull CommandSection commandSection) {
        super(
                commandSection.getName(),
                commandSection.getDescription(),
                commandSection.getUsage(),
                commandSection.getAliases()
        );
        this.commandSection = commandSection;
    }

    // ── Abstract hooks ────────────────────────────────────────────────────────

    /**
     * Command-specific execution logic.
     *
     * @throws CommandError for user-facing errors (localized by the base class)
     */
    protected abstract void onInvocation(CommandSender sender, String alias, String[] args);

    /**
     * Tab completion logic.
     *
     * @return suggestions to present to the sender
     * @throws CommandError for contextual validation errors
     */
    protected abstract List<String> onTabCompletion(CommandSender sender, String alias, String[] args);

    // ── Bukkit overrides ──────────────────────────────────────────────────────

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        return executeAndHandleErrors(() -> {
            onInvocation(sender, alias, args);
            return true;
        }, false, sender, alias, args);
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        return executeAndHandleErrors(
                () -> onTabCompletion(sender, alias, args),
                List.of(), sender, alias, args
        );
    }

    // ── Argument parsing ──────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    protected <T extends Enum<?>> T enumParameter(String[] args, int index, Class<T> enumClass) {
        var enumInfo = enumConstantsCache.computeIfAbsent(enumClass, EnumInfoImpl::new);
        var constant = enumInfo.enumConstantByLowerCaseName.get(resolveArg(args, index).toLowerCase());
        if (constant == null) {
            throw new CommandError(index, ErrorType.MALFORMED_ENUM, enumInfo);
        }
        return (T) constant;
    }

    protected <T extends Enum<?>> T enumParameterOrElse(String[] args, int index, Class<T> enumClass, T fallback) {
        return orElse(() -> enumParameter(args, index, enumClass), fallback);
    }

    protected Player playerParameter(String[] args, int index) {
        var player = Bukkit.getPlayer(resolveArg(args, index));
        if (player == null) {
            throw new CommandError(index, ErrorType.PLAYER_NOT_ONLINE);
        }
        return player;
    }

    protected Player playerParameterOrElse(String[] args, int index, Player fallback) {
        return orElse(() -> playerParameter(args, index), fallback);
    }

    protected String stringParameter(String[] args, int index) {
        return resolveArg(args, index);
    }

    protected OfflinePlayer offlinePlayerParameter(String[] args, int index, boolean mustHavePlayed) {
        var player = Bukkit.getOfflinePlayer(resolveArg(args, index));
        if (mustHavePlayed && !player.hasPlayedBefore()) {
            throw new CommandError(index, ErrorType.PLAYER_UNKNOWN);
        }
        return player;
    }

    protected OfflinePlayer offlinePlayerParameterOrElse(String[] args, int index, boolean mustHavePlayed, OfflinePlayer fallback) {
        return orElse(() -> offlinePlayerParameter(args, index, mustHavePlayed), fallback);
    }

    protected UUID uuidParameter(String[] args, int index) {
        try {
            return UUID.fromString(resolveArg(args, index));
        } catch (IllegalArgumentException e) {
            throw new CommandError(index, ErrorType.MALFORMED_UUID);
        }
    }

    protected UUID uuidParameterOrElse(String[] args, int index, UUID fallback) {
        return orElse(() -> uuidParameter(args, index), fallback);
    }

    protected Integer integerParameter(String[] args, int index) {
        try {
            return Integer.parseInt(resolveArg(args, index));
        } catch (NumberFormatException e) {
            throw new CommandError(index, ErrorType.MALFORMED_INTEGER);
        }
    }

    protected Integer integerParameterOrElse(String[] args, int index, Integer fallback) {
        return orElse(() -> integerParameter(args, index), fallback);
    }

    protected Long longParameter(String[] args, int index) {
        try {
            return Long.parseLong(resolveArg(args, index));
        } catch (NumberFormatException e) {
            throw new CommandError(index, ErrorType.MALFORMED_LONG);
        }
    }

    protected Long longParameterOrElse(String[] args, int index, Long fallback) {
        return orElse(() -> longParameter(args, index), fallback);
    }

    protected Double doubleParameter(String[] args, int index) {
        try {
            return Double.parseDouble(resolveArg(args, index));
        } catch (NumberFormatException e) {
            throw new CommandError(index, ErrorType.MALFORMED_DOUBLE);
        }
    }

    protected Double doubleParameterOrElse(String[] args, int index, Double fallback) {
        return orElse(() -> doubleParameter(args, index), fallback);
    }

    protected Float floatParameter(String[] args, int index) {
        try {
            return Float.parseFloat(resolveArg(args, index));
        } catch (NumberFormatException e) {
            throw new CommandError(index, ErrorType.MALFORMED_FLOAT);
        }
    }

    protected Float floatParameterOrElse(String[] args, int index, Float fallback) {
        return orElse(() -> floatParameter(args, index), fallback);
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private <T> T orElse(Supplier<T> lookup, T fallback) {
        try {
            return lookup.get();
        } catch (CommandError error) {
            if (error.errorType == ErrorType.MISSING_ARGUMENT) return fallback;
            throw error;
        }
    }

    private String resolveArg(String[] args, int index) {
        if (index < 0) throw new IllegalArgumentException("Argument indices start at zero");
        if (index >= args.length) throw new CommandError(index, ErrorType.MISSING_ARGUMENT);
        return args[index];
    }

    private void sendComponent(CommandSender sender, Component component) {
        sender.sendMessage(LEGACY_SERIALIZER.serialize(component));
    }

    private <T> T executeAndHandleErrors(
            Supplier<T> action,
            T fallback,
            CommandSender sender,
            String alias,
            String[] args
    ) {
        try {
            return action.get();
        } catch (CommandError error) {
            handleError(error, sender, alias, args);
            return fallback;
        } catch (Exception exception) {
            var context = new ErrorContext(sender, alias, args, null);
            sendComponent(sender, commandSection.getInternalErrorMessage(context));
            LOG.warn("Error executing command", exception);
            return fallback;
        }
    }

    private void handleError(CommandError error, CommandSender sender, String alias, String[] args) {
        var context = new ErrorContext(sender, alias, args, error.argumentIndex);
        var message = switch (error.errorType) {
            case MALFORMED_DOUBLE   -> commandSection.getMalformedDoubleMessage(context);
            case MALFORMED_FLOAT    -> commandSection.getMalformedFloatMessage(context);
            case MALFORMED_LONG     -> commandSection.getMalformedLongMessage(context);
            case MALFORMED_INTEGER  -> commandSection.getMalformedIntegerMessage(context);
            case MALFORMED_UUID     -> commandSection.getMalformedUuidMessage(context);
            case MALFORMED_ENUM     -> commandSection.getMalformedEnumMessage(context, (EnumInfo) error.parameter);
            case MISSING_ARGUMENT   -> commandSection.getMissingArgumentMessage(context);
            case NOT_A_PLAYER       -> commandSection.getNotAPlayerMessage(context);
            case NOT_A_CONSOLE      -> commandSection.getNotAConsoleMessage(context);
            case PLAYER_UNKNOWN     -> commandSection.getPlayerUnknownMessage(context);
            case PLAYER_NOT_ONLINE  -> commandSection.getPlayerNotOnlineMessage(context);
        };
        sendComponent(sender, message);
    }
}
