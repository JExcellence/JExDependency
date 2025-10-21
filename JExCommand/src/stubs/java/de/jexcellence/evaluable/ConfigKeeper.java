package de.jexcellence.evaluable;

import de.jexcellence.evaluable.section.ACommandSection;

/**
 * Minimal configuration keeper that instantiates section classes via their
 * zero-argument constructor. The production implementation applies YAML-backed
 * mappings, which are not required for the behaviour exercised in the unit
 * tests.
 */
public class ConfigKeeper<T extends ACommandSection> {

    private final Class<T> sectionClass;

    public ConfigKeeper(ConfigManager manager, String fileName, Class<T> sectionClass) {
        this.sectionClass = sectionClass;
    }

    public T mapSection(String path) throws ReflectiveOperationException {
        return this.sectionClass.getDeclaredConstructor().newInstance();
    }
}
