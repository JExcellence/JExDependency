package com.raindropcentral.commands.v2;

import org.jetbrains.annotations.NotNull;

/**
 * Functional interface executed when a JExCommand 2.0 command path matches.
 *
 * <p>The handler receives a fully parsed {@link CommandContext}; every argument
 * declared in the YAML schema is guaranteed to be present and typed (unless the
 * schema marked it optional without a {@code defaultValue}, in which case
 * {@link CommandContext#raw(String)} may return {@code null}).
 *
 * <p>Handlers are typically registered by path when building a command tree:
 *
 * <pre>
 * Map.of(
 *     "economy.give",  (CommandHandler) ctx -> { ... },
 *     "economy.take",  (CommandHandler) ctx -> { ... },
 *     "economy.reset", (CommandHandler) ctx -> { ... })
 * </pre>
 *
 * <p>Implementations should delegate long-running work to an executor; the
 * dispatcher invokes handlers on the calling thread (usually the Bukkit main
 * thread for command execution, or an async Bukkit worker for tab completion).
 *
 * @author JExcellence
 * @since 2.0.0
 */
@FunctionalInterface
public interface CommandHandler {

    /**
     * Executes the command with the given fully parsed context.
     *
     * @param context parsed arguments + sender metadata
     */
    void handle(@NotNull CommandContext context);
}
