package com.raindropcentral.rplatform.database.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

/**
 * JPA attribute converter for mapping {@link World} objects to their name representations
 * for database storage and vice versa.
 * <p>
 * This converter enables seamless persistence of Bukkit {@code World} objects
 * by converting them to their name for storage, and reconstructing
 * the world from the stored name when reading from the database.
 * </p>
 *
 * <p>
 * The converter is automatically applied to all {@code World} attributes in JPA entities.
 * </p>
 *
 * @version 1.0.0
 * @since TBD
 * @author JExcellence
 */
@Converter(autoApply = true)
public class WorldConverter implements AttributeConverter<World, String> {

    /**
     * Converts the given {@link World} object to its name for database storage.
     *
     * @param attribute the {@code World} object to be converted; must not be {@code null}
     * @return the name of the {@code World} object, suitable for storage in the database
     */
    @Override
    public String convertToDatabaseColumn(
            final @NotNull World attribute
    ) {
        return attribute.getName();
    }

    /**
     * Converts the given world name from the database to its corresponding {@link World} object.
     *
     * @param dbData the name of the {@code World} as stored in the database; must not be {@code null}
     * @return the {@code World} object corresponding to the given database value, or {@code null} if not found
     */
    @Override
    public World convertToEntityAttribute(
            final @NotNull String dbData
    ) {
        return Bukkit.getWorld(dbData);
    }
}
