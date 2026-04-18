package com.raindropcentral.commands.v2.argument;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Declarative argument specification from the YAML schema.
 *
 * <pre>
 * argumentSchema:
 *   - { name: target, type: online_player,   required: true }
 *   - { name: amount, type: positive_double, required: true }
 *   - { name: note,   type: quoted_string,   required: false, defaultValue: "" }
 * </pre>
 *
 * <p>A spec is a pure value object — resolution against an {@link ArgumentTypeRegistry}
 * happens at load time and produces a {@code ResolvedArgument} internally.
 *
 * @param name         placeholder id referenced from the handler (required)
 * @param typeId       argument type id ({@code string}, {@code enum(...)}, custom ids)
 * @param required     whether the argument must be present
 * @param defaultValue raw default applied when {@code required=false} and no token given
 * @param description  optional hint shown in help output
 * @author JExcellence
 * @since 2.0.0
 */
public record ArgumentSpec(
        @NotNull String name,
        @NotNull String typeId,
        boolean required,
        @Nullable String defaultValue,
        @Nullable String description
) {

    /**
     * Creates a required argument without a default value or description.
     */
    public static @NotNull ArgumentSpec required(@NotNull String name, @NotNull String typeId) {
        return new ArgumentSpec(name, typeId, true, null, null);
    }

    /**
     * Creates an optional argument with a raw default value.
     */
    public static @NotNull ArgumentSpec optional(@NotNull String name, @NotNull String typeId,
                                                  @Nullable String defaultValue) {
        return new ArgumentSpec(name, typeId, false, defaultValue, null);
    }
}
