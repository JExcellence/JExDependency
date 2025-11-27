package de.jexcellence.economy.command.player.currencylog;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Enumerates the supported sub-commands for the {@code /pcurrencylog} command and
 * exposes structured metadata used by help generation, tab completion, and
 * permission validation.
 *
 * <p>
 * Each action provides:
 * </p>
 * <ul>
 *     <li>A canonical command keyword used during parsing</li>
 *     <li>A translation key that documents the usage description</li>
 *     <li>A list of required arguments (in order) for validation and UX hints</li>
 *     <li>An optional permission node when elevated privileges are required</li>
 * </ul>
 *
 * <p>
 * Helper methods offer convenient lookups from user supplied input and enable
 * consumers to align command output with localisation files and configuration
 * metadata.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 1.0.0
 */
public enum ECurrencyLogAction {

    VIEW(
        "view",
        "currency_log.command_view",
        List.of(),
        "pcurrencylog.command"
    ),
    FILTER(
        "filter",
        "currency_log.command_filter",
        List.of("filter-type", "filter-value"),
        "pcurrencylog.command"
    ),
    STATS(
        "stats",
        "currency_log.command_stats",
        List.of(),
        "pcurrencylog.command"
    ),
    CLEAR(
        "clear",
        "currency_log.command_clear",
        List.of(),
        "pcurrencylog.command"
    ),
    EXPORT(
        "export",
        "currency_log.command_export",
        List.of(),
        "jexeconomy.admin.export"
    ),
    HELP(
        "help",
        "currency_log.command_help",
        List.of(),
        "pcurrencylog.command"
    ),
    DETAILS(
        "details",
        "currency_log.command_details",
        List.of("log-id"),
        "pcurrencylog.command"
    );

    private final String commandKeyword;
    private final String usageTranslationKey;
    private final List<String> requiredArguments;
    private final String requiredPermissionNode;

    ECurrencyLogAction(
        final @NotNull String commandKeyword,
        final @NotNull String usageTranslationKey,
        final @NotNull List<String> requiredArguments,
        final @Nullable String requiredPermissionNode
    ) {
        if (commandKeyword.isBlank()) {
            throw new IllegalArgumentException("Command keyword must not be blank");
        }
        if (usageTranslationKey.isBlank()) {
            throw new IllegalArgumentException("Usage translation key must not be blank");
        }

        this.commandKeyword = commandKeyword;
        this.usageTranslationKey = usageTranslationKey;
        this.requiredArguments = Collections.unmodifiableList(requiredArguments);
        this.requiredPermissionNode = requiredPermissionNode;
    }

    public @NotNull String getCommandKeyword() {
        return this.commandKeyword;
    }

    public @NotNull String getUsageTranslationKey() {
        return this.usageTranslationKey;
    }

    public @NotNull List<String> getRequiredArguments() {
        return this.requiredArguments;
    }

    public @NotNull Optional<String> getRequiredPermissionNode() {
        return Optional.ofNullable(this.requiredPermissionNode);
    }

    public boolean requiresAdditionalPermission() {
        return this.requiredPermissionNode != null;
    }

    public static @NotNull Optional<ECurrencyLogAction> fromCommandKeyword(final @Nullable String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Optional.empty();
        }

        final String normalised = keyword.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
            .filter(action -> action.getCommandKeyword().equalsIgnoreCase(normalised))
            .findFirst();
    }
}
