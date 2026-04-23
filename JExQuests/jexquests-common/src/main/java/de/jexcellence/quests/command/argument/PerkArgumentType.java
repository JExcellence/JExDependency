package de.jexcellence.quests.command.argument;

import com.raindropcentral.commands.v2.argument.ArgumentType;
import de.jexcellence.quests.database.entity.Perk;
import de.jexcellence.quests.service.PerkService;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * JExCommand 2.0 argument type {@code perk}. Parses to the perk
 * identifier after validating the perk exists in the registry.
 */
public final class PerkArgumentType {

    private PerkArgumentType() {
    }

    public static @NotNull ArgumentType<String> of(@NotNull PerkService service) {
        final IdentifierCompletionCache cache = new IdentifierCompletionCache(() -> {
            try {
                return service.perks().findEnabledAsync()
                        .orTimeout(200L, TimeUnit.MILLISECONDS)
                        .join()
                        .stream()
                        .map(Perk::getIdentifier)
                        .toList();
            } catch (final RuntimeException ex) {
                return List.of();
            }
        });
        return ArgumentType.custom(
                "perk",
                String.class,
                (sender, raw) -> service.perks().findByIdentifier(raw)
                        .map(perk -> ArgumentType.ParseResult.ok(perk.getIdentifier()))
                        .orElseGet(() -> ArgumentType.ParseResult.err(
                                "perk.not-found",
                                Map.of("perk", raw))),
                (sender, partial) -> cache.matching(partial)
        );
    }
}
