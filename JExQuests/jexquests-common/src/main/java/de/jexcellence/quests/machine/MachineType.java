package de.jexcellence.quests.machine;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Immutable description of a machine kind (blueprint). Loaded from
 * {@code plugins/JExQuests/machines/*.yml} by
 * {@link de.jexcellence.quests.machine.MachineDefinitionLoader};
 * consulted at runtime by the placement / interaction flow and by
 * the {@code /machine info} command.
 *
 * @param identifier stable identifier (filename stem)
 * @param displayName MiniMessage-formatted display name
 * @param description short description
 * @param category category key (e.g. "industrial", "storage")
 * @param iconMaterial Bukkit material key for the UI icon
 * @param width X extent in blocks
 * @param height Y extent in blocks
 * @param depth Z extent in blocks
 * @param properties plugin-specific runtime knobs (recipe id, tick rate, etc.)
 */
public record MachineType(
        @NotNull String identifier,
        @NotNull String displayName,
        @NotNull String description,
        @NotNull String category,
        @NotNull String iconMaterial,
        int width,
        int height,
        int depth,
        @NotNull Map<String, Object> properties
) {
    public MachineType {
        properties = Map.copyOf(properties);
    }
}
