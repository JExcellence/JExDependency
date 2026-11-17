package de.jexcellence.dependency.loader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The contents of a plugin's {@code dependencies.yml}.
 *
 * <pre>{@code
 * repositories:                      # tried after Maven Central, in this order
 *   - https://repo.example.org/releases
 * community-repositories: false      # true restores the built-in JitPack, PaperMC and other repositories
 * relocation-prefix: com.example.myplugin.libs   # prefix your Shadow relocate(...) lines use
 * relocation-excludes:               # packages the automatic relocation leaves alone
 *   - org.hibernate
 * relocations:                       # explicit relocations; replace the automatic ones
 *   - com.google.gson => com.example.myplugin.libs.gson
 * dependencies:
 *   - com.github.ben-manes.caffeine:caffeine:3.2.2
 *   - com.example:tool:1.0 @ https://repo.example.org/releases   # only from this repository
 *   - com.example:other:2.0 sha256=<64 hex characters>         # only this exact jar
 * }</pre>
 *
 * @param entries               declared dependencies in file order
 * @param repositories          extra repository base URLs without trailing slashes, tried after Maven Central
 * @param communityRepositories whether the built-in community repositories are searched after Central
 * @param relocationExcludes    package prefixes the automatic relocation must not touch
 * @param relocationPrefix      package prefix for automatic relocation, or {@code null} for the server default
 * @param relocations           explicit relocations from original to relocated package
 */
public record DependencyDescriptor(
        @NotNull List<DependencyEntry> entries,
        @NotNull List<String> repositories,
        boolean communityRepositories,
        @NotNull List<String> relocationExcludes,
        @Nullable String relocationPrefix,
        @NotNull Map<String, String> relocations
) {

    /** A descriptor that declares nothing. */
    public static final DependencyDescriptor EMPTY = new DependencyDescriptor(List.of(), List.of(), false, List.of());

    /**
     * Copies the collections so the descriptor cannot change after creation.
     */
    public DependencyDescriptor {
        entries = List.copyOf(entries);
        repositories = List.copyOf(repositories);
        relocationExcludes = List.copyOf(relocationExcludes);
        relocations = Collections.unmodifiableMap(new LinkedHashMap<>(relocations));
    }

    /**
     * Creates a descriptor without relocation settings.
     *
     * @param entries               declared dependencies in file order
     * @param repositories          extra repository base URLs, tried after Maven Central
     * @param communityRepositories whether the built-in community repositories are searched after Central
     * @param relocationExcludes    package prefixes the automatic relocation must not touch
     */
    public DependencyDescriptor(
            @NotNull final List<DependencyEntry> entries,
            @NotNull final List<String> repositories,
            final boolean communityRepositories,
            @NotNull final List<String> relocationExcludes
    ) {
        this(entries, repositories, communityRepositories, relocationExcludes, null, Map.of());
    }

    /**
     * Returns the declared coordinates as {@code group:artifact:version[:classifier]} strings, without repository
     * pins or hashes.
     *
     * @return coordinates in file order
     */
    public @NotNull List<String> dependencies() {
        return entries.stream().map(entry -> entry.coordinate().toGavString()).toList();
    }

    /**
     * Tells whether the descriptor declares no dependencies.
     *
     * @return {@code true} when there are no entries
     */
    public boolean isEmpty() {
        return entries.isEmpty();
    }
}
