package de.jexcellence.quests.command.argument;

import com.raindropcentral.commands.v2.argument.ArgumentType;
import de.jexcellence.quests.database.entity.RankTree;
import de.jexcellence.quests.service.RankService;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * JExCommand 2.0 argument type {@code rank-tree}. Parses the token
 * to the tree identifier after validating the tree exists via
 * {@link RankService}.
 */
public final class RankTreeArgumentType {

    private RankTreeArgumentType() {
    }

    public static @NotNull ArgumentType<String> of(@NotNull RankService service) {
        final IdentifierCompletionCache cache = new IdentifierCompletionCache(() -> {
            try {
                return service.trees().findEnabledAsync()
                        .orTimeout(200L, TimeUnit.MILLISECONDS)
                        .join()
                        .stream()
                        .map(RankTree::getIdentifier)
                        .toList();
            } catch (final RuntimeException ex) {
                return List.of();
            }
        });
        return ArgumentType.custom(
                "rank-tree",
                String.class,
                (sender, raw) -> service.trees().findByIdentifier(raw)
                        .map(tree -> ArgumentType.ParseResult.ok(tree.getIdentifier()))
                        .orElseGet(() -> ArgumentType.ParseResult.err(
                                "rank.path-not-found",
                                Map.of("path", raw))),
                (sender, partial) -> cache.matching(partial)
        );
    }
}
