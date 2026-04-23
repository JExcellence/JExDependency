package de.jexcellence.quests.command.argument;

import com.raindropcentral.commands.v2.argument.ArgumentType;
import de.jexcellence.quests.database.entity.Quest;
import de.jexcellence.quests.service.QuestService;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * JExCommand 2.0 argument type {@code quest} — validates the raw
 * token against {@link QuestService#quests()} and suggests enabled
 * quest identifiers in tab completion.
 *
 * <p>YAML schema:
 * <pre>
 * argumentSchema:
 *   - { name: quest, type: quest, required: true }
 * </pre>
 *
 * <p>On miss: emits {@code quest.not-found} with {@code {quest}}
 * equal to the raw token.
 */
public final class QuestArgumentType {

    private QuestArgumentType() {
    }

    public static @NotNull ArgumentType<String> of(@NotNull QuestService service) {
        final IdentifierCompletionCache cache = new IdentifierCompletionCache(() -> {
            try {
                return service.quests().findEnabledAsync()
                        .orTimeout(200L, TimeUnit.MILLISECONDS)
                        .join()
                        .stream()
                        .map(Quest::getIdentifier)
                        .toList();
            } catch (final RuntimeException ex) {
                return List.of();
            }
        });
        return ArgumentType.custom(
                "quest",
                String.class,
                (sender, raw) -> service.quests().findByIdentifier(raw)
                        .map(q -> ArgumentType.ParseResult.ok(q.getIdentifier()))
                        .orElseGet(() -> ArgumentType.ParseResult.err(
                                "quest.not-found",
                                Map.of("quest", raw))),
                (sender, partial) -> cache.matching(partial)
        );
    }
}
