package de.jexcellence.jexplatform.database.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

/**
 * JPA converter that persists Bukkit {@link World} references as their
 * world name.
 *
 * <p>Returns {@code null} when the world is not currently loaded.
 *
 * @author JExcellence
 * @since 1.0.0
 */
@Converter(autoApply = true)
public class WorldConverter implements AttributeConverter<World, String> {

    @Override
    public String convertToDatabaseColumn(@Nullable World attribute) {
        return attribute == null ? null : attribute.getName();
    }

    @Override
    public World convertToEntityAttribute(@Nullable String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return Bukkit.getWorld(dbData.trim());
    }
}
