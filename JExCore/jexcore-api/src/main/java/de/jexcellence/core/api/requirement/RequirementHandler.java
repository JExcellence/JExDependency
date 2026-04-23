package de.jexcellence.core.api.requirement;

import org.jetbrains.annotations.NotNull;

/**
 * Synchronous per-type requirement handler. Invoked on the Bukkit main
 * thread by the evaluator — free to consult player state, scoreboards,
 * permissions, etc. without scheduling.
 */
@FunctionalInterface
public interface RequirementHandler {

    @NotNull RequirementResult evaluate(@NotNull Requirement requirement, @NotNull RequirementContext context);
}
