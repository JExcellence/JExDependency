/**
 * JPA converters for serializing Bukkit objects to database columns.
 *
 * <p>This package contains AttributeConverter implementations that handle the persistence
 * of complex Bukkit objects like Location, World, ItemStack lists, and Material lists.
 * These converters enable seamless storage and retrieval of Minecraft-specific data types
 * in the oneblock system's database entities.</p>
 *
 * <p>Key converters include:
 * <ul>
 *   <li>{@link de.jexcellence.oneblock.database.converter.LocationConverter} - Converts Bukkit Location to semicolon-delimited string</li>
 *   <li>{@link de.jexcellence.oneblock.database.converter.WorldConverter} - Converts Bukkit World to world name</li>
 *   <li>{@link de.jexcellence.oneblock.database.converter.ItemStackListConverter} - Converts ItemStack lists to Base64 payload</li>
 *   <li>{@link de.jexcellence.oneblock.database.converter.MaterialListConverter} - Converts Material lists to semicolon-delimited string</li>
 * </ul>
 * </p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.0
 */
package de.jexcellence.oneblock.database.converter;