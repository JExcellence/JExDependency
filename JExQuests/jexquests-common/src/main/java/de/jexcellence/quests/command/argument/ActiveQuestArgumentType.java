package de.jexcellence.quests.command.argument;

import com.raindropcentral.commands.v2.argument.ArgumentType;
import de.jexcellence.quests.database.entity.PlayerQuestProgress;
import de.jexcellence.quests.service.QuestService;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * JExCommand 2.0 argument type {@code quest-active} — same parse
 * semantics as {@code quest} (validates the identifier against the
 * global registry) but tab completion is filtered to the current
 * player's <em>active</em> quests only. Useful for
 * {@code /quest abandon} and {@code /quest track} where suggesting
 * every enabled quest in the registry would be noise.
 *
 * <p>Non-player senders see an empty completion list — console has
 * no active quests.
 */
public final class ActiveQuestArgumentType {

    private ActiveQuestArgumentType() {
    }

    /**
     * Creates an argument type for active quests.
     */
    public static @NotNull ArgumentType<String> of(@NotNull QuestService service) {
        return ArgumentType.custom(
                "quest-active",
                String.class,
                (sender, raw) -> service.quests().findByIdentifier(raw)
                        .map(quest -> ArgumentType.ParseResult.ok(quest.getIdentifier()))
                        .orElseGet(() -> ArgumentType.ParseResult.err(
                                "quest.not-found",
                                Map.of("quest", raw))),
                (sender, partial) -> {
                    if (!(sender instanceof Player player)) return List.of();
                    try {
                        final List<PlayerQuestProgress> active = service
                                .activeForPlayerAsync(player.getUniqueId())
                                .orTimeout(200L, TimeUnit.MILLISECONDS)
                                .join();
                        final String lower = partial.toLowerCase(Locale.ROOT);
                        return active.stream()
                                .map(PlayerQuestProgress::getQuestIdentifier)
                                .filter(id -> id.toLowerCase(Locale.ROOT).startsWith(lower))
                                .sorted()
                                .toList();
                    } catch (final RuntimeException ex) {
                        return List.of();
                    }
                }
        );
    }
}
