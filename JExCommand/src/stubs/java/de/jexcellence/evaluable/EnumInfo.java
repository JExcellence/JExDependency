package de.jexcellence.evaluable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Minimal enum metadata holder used by the test doubles. Only the type itself
 * is required for the assertions performed in the unit tests.
 */
public class EnumInfo {

    public final Map<String, Enum<?>> enumConstantByLowerCaseName = new HashMap<>();

    public EnumInfo(Class<? extends Enum<?>> enumType) {
        if (enumType != null) {
            for (Enum<?> constant : enumType.getEnumConstants()) {
                enumConstantByLowerCaseName.put(constant.name().toLowerCase(Locale.ROOT), constant);
            }
        }
    }
}
