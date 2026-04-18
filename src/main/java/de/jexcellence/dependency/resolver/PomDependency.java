package de.jexcellence.dependency.resolver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * A single {@code <dependency>} entry as parsed from a Maven POM file.
 *
 * <p>Version and coordinate fields may still contain unresolved property references
 * (e.g. {@code ${project.version}}) at construction time; the
 * {@link TransitiveDependencyResolver} substitutes those during resolution.
 *
 * @param groupId    Maven group identifier (may contain {@code ${…}} placeholders)
 * @param artifactId Maven artifact identifier
 * @param version    version string or {@code null} when managed externally
 * @param scope      Maven scope string; defaults to {@code compile} when absent in the POM
 * @param optional   {@code true} when the POM marks the entry {@code <optional>true</optional>}
 * @param exclusions set of {@code "groupId:artifactId"} pairs that must not be pulled in
 *                   transitively through this dependency
 */
public record PomDependency(
        @NotNull String groupId,
        @NotNull String artifactId,
        @Nullable String version,
        @NotNull String scope,
        boolean optional,
        @NotNull Set<String> exclusions
) {

    /** Default scope when {@code <scope>} is absent in the POM. */
    public static final String SCOPE_COMPILE  = "compile";
    public static final String SCOPE_RUNTIME  = "runtime";
    public static final String SCOPE_TEST     = "test";
    public static final String SCOPE_PROVIDED = "provided";
    public static final String SCOPE_SYSTEM   = "system";
    /** Special scope used in {@code <dependencyManagement>} to import a BOM. */
    public static final String SCOPE_IMPORT   = "import";

    /**
     * Returns {@code true} when this dependency should be placed on the runtime classpath.
     *
     * <p>Only {@code compile} and {@code runtime} scoped, non-optional dependencies qualify.
     * {@code test}, {@code provided}, {@code system}, and {@code import} entries are always
     * excluded.
     */
    public boolean isRuntimeIncluded() {
        return !optional
                && (SCOPE_COMPILE.equals(scope) || SCOPE_RUNTIME.equals(scope));
    }

    /**
     * Returns the deduplication key for this dependency ({@code groupId:artifactId}).
     * Version is intentionally omitted so that the same library at different versions still
     * compares equal — the first version seen wins during resolution (Maven's nearest-wins rule).
     */
    public @NotNull String toKey() {
        return groupId + ":" + artifactId;
    }
}
