package de.jexcellence.core.api.requirement;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Public entry point for evaluating requirements. Obtained via
 * {@code Bukkit.getServicesManager().load(RequirementEvaluator.class)}
 * once JExCore's common module has registered its default
 * implementation.
 *
 * <p>Third-party plugins use {@link #registerHandler(String, RequirementHandler)}
 * to attach handlers for {@link Requirement.Custom} entries keyed on
 * the {@code type} string.
 */
public interface RequirementEvaluator {

    /** Async evaluate — schedules onto the Bukkit main thread and resolves the returned future. */
    @NotNull CompletableFuture<RequirementResult> evaluate(
            @NotNull Requirement requirement,
            @NotNull RequirementContext context
    );

    /** Synchronous evaluate on the current (main) thread. */
    @NotNull RequirementResult evaluateSync(
            @NotNull Requirement requirement,
            @NotNull RequirementContext context
    );

    void registerHandler(@NotNull String type, @NotNull RequirementHandler handler);

    void unregisterHandler(@NotNull String type);

    /** Convenience lookup via {@code ServicesManager}. */
    static @Nullable RequirementEvaluator get() {
        return Bukkit.getServicesManager().load(RequirementEvaluator.class);
    }
}
