package de.jexcellence.dependency.remapper;

import org.objectweb.asm.commons.Remapper;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ASM {@link Remapper} that relocates class internal names using longest-prefix matching.
 *
 * <p>Mappings are supplied in dot notation ({@code "com.example"}) and stored internally in slash
 * notation ({@code "com/example"}) as required by ASM. Longer prefixes take precedence over shorter
 * ones, so {@code "com.example.sub"} will shadow a mapping on {@code "com.example"} when both apply.
 */
final class PrefixRelocationRemapper extends Remapper {

    /**
     * Internal slash-notation prefix mappings sorted longest-first for correct precedence.
     * e.g. {@code "com/example" -> "my/prefix/com/example"}.
     */
    private final Map<String, String> prefixMapInternal;

    /**
     * Constructs the remapper from dot-notation mappings.
     *
     * @param mappings {@code fromPackage -> toPackage} in dot notation
     */
    PrefixRelocationRemapper(final Map<String, String> mappings) {
        this.prefixMapInternal = new LinkedHashMap<>();
        mappings.entrySet()
                .stream()
                .sorted(Comparator.comparingInt(e -> -e.getKey().length())) // longest prefix first
                .forEach(e -> this.prefixMapInternal.put(
                        e.getKey().replace('.', '/'),
                        e.getValue().replace('.', '/')
                ));
    }

    @Override
    public String map(final String internalName) {
        if (internalName == null) return null;
        if (internalName.startsWith("[")) {
            // Let Remapper handle array descriptors via its default logic
            return super.map(internalName);
        }
        return relocateInternal(internalName);
    }

    @Override
    public String mapPackageName(final String name) {
        if (name == null) return null;
        final String internal = name.replace('.', '/');
        return relocateInternal(internal).replace('/', '.');
    }

    private String relocateInternal(final String internal) {
        for (final Map.Entry<String, String> e : prefixMapInternal.entrySet()) {
            final String from = e.getKey();
            if (internal.equals(from) || internal.startsWith(from + "/")) {
                return e.getValue() + internal.substring(from.length());
            }
        }
        return internal;
    }
}
