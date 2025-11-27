package com.raindropcentral.rdq.shared.error;

import de.jexcellence.jextranslate.api.Placeholder;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;

/**
 * Utility class for handling RDQ errors and sending localized error messages.
 *
 * <p>Uses pattern matching to convert {@link RDQError} instances into translation
 * keys and placeholders for localized error messages.
 *
 * <p>Example usage:
 * <pre>{@code
 * try {
 *     service.doSomething();
 * } catch (RDQException e) {
 *     ErrorHandler.sendError(player, e, translations);
 * }
 * }</pre>
 *
 * @see RDQError
 * @see RDQException
 */
public final class ErrorHandler {

    private ErrorHandler() {
    }

    @NotNull
    public static TranslationKey getTranslationKey(@NotNull RDQError error) {
        var key = switch (error) {
            case RDQError.NotFound n -> "error.not_found";
            case RDQError.InsufficientFunds i -> "error.insufficient_funds";
            case RDQError.OnCooldown o -> "error.on_cooldown";
            case RDQError.RequirementsNotMet r -> "error.requirements_not_met";
            case RDQError.SelfTargeting s -> "error.self_targeting";
            case RDQError.AlreadyExists a -> "error.already_exists";
            case RDQError.Expired e -> "error.expired";
            case RDQError.NotUnlocked n -> "error.not_unlocked";
            case RDQError.PermissionDenied p -> "error.permission_denied";
            case RDQError.InvalidState i -> "error.invalid_state";
            case RDQError.InvalidAmount a -> "error.invalid_amount";
        };
        return TranslationKey.of(key);
    }

    @NotNull
    public static List<Placeholder> getPlaceholders(@NotNull RDQError error) {
        return switch (error) {
            case RDQError.NotFound(var type, var id) -> List.of(
                Placeholder.of("type", type),
                Placeholder.of("id", id)
            );
            case RDQError.InsufficientFunds(var required, var available) -> List.of(
                Placeholder.of("required", required.toPlainString()),
                Placeholder.of("available", available.toPlainString())
            );
            case RDQError.OnCooldown(var remaining) -> List.of(
                Placeholder.of("remaining", formatDuration(remaining)),
                Placeholder.of("seconds", remaining.toSeconds())
            );
            case RDQError.RequirementsNotMet(var missing) -> List.of(
                Placeholder.of("missing", String.join(", ", missing)),
                Placeholder.of("count", missing.size())
            );
            case RDQError.SelfTargeting() -> List.of();
            case RDQError.AlreadyExists(var type, var id) -> List.of(
                Placeholder.of("type", type),
                Placeholder.of("id", id)
            );
            case RDQError.Expired(var type, var id) -> List.of(
                Placeholder.of("type", type),
                Placeholder.of("id", id)
            );
            case RDQError.NotUnlocked(var type, var id) -> List.of(
                Placeholder.of("type", type),
                Placeholder.of("id", id)
            );
            case RDQError.PermissionDenied(var permission) -> List.of(
                Placeholder.of("permission", permission)
            );
            case RDQError.InvalidState(var message) -> List.of(
                Placeholder.of("message", message)
            );
            case RDQError.InvalidAmount(var min, var max) -> List.of(
                Placeholder.of("min", min.toPlainString()),
                Placeholder.of("max", max.toPlainString())
            );
        };
    }

    public static void sendError(@NotNull Player player, @NotNull RDQError error, @NotNull TranslationService translations) {
        var key = getTranslationKey(error);
        var placeholders = getPlaceholders(error);

        var builder = translations.create(key, player);
        for (var placeholder : placeholders) {
            builder = builder.with(placeholder);
        }
        builder.send();
    }

    public static void sendError(@NotNull Player player, @NotNull RDQException exception, @NotNull TranslationService translations) {
        sendError(player, exception.getError(), translations);
    }

    @NotNull
    public static String formatDuration(@NotNull Duration duration) {
        var seconds = duration.toSeconds();
        if (seconds < 60) {
            return seconds + "s";
        }
        var minutes = duration.toMinutes();
        if (minutes < 60) {
            var remainingSeconds = seconds % 60;
            return remainingSeconds > 0 ? minutes + "m " + remainingSeconds + "s" : minutes + "m";
        }
        var hours = duration.toHours();
        var remainingMinutes = minutes % 60;
        return remainingMinutes > 0 ? hours + "h " + remainingMinutes + "m" : hours + "h";
    }

    @NotNull
    public static String getErrorMessage(@NotNull RDQError error) {
        return switch (error) {
            case RDQError.NotFound(var type, var id) -> type + " not found: " + id;
            case RDQError.InsufficientFunds(var req, var avail) -> "Insufficient funds: required " + req + ", available " + avail;
            case RDQError.OnCooldown(var remaining) -> "On cooldown: " + formatDuration(remaining) + " remaining";
            case RDQError.RequirementsNotMet(var missing) -> "Requirements not met: " + String.join(", ", missing);
            case RDQError.SelfTargeting() -> "Cannot target yourself";
            case RDQError.AlreadyExists(var type, var id) -> type + " already exists: " + id;
            case RDQError.Expired(var type, var id) -> type + " has expired: " + id;
            case RDQError.NotUnlocked(var type, var id) -> type + " not unlocked: " + id;
            case RDQError.PermissionDenied(var perm) -> "Permission denied: " + perm;
            case RDQError.InvalidState(var msg) -> "Invalid state: " + msg;
            case RDQError.InvalidAmount(var min, var max) -> "Invalid amount: must be between " + min + " and " + max;
        };
    }
}
