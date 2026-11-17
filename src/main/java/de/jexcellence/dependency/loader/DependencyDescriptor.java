package de.jexcellence.dependency.loader;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * The contents of a plugin's {@code dependencies.yml}.
 *
 * <pre>{@code
 * repositories:                      # tried after Maven Central, in this order
 *   - https://repo.example.org/releases
 * community-repositories: false      # true restores the built-in JitPack, PaperMC and other repositories
 * relocation-excludes:               # packages the automatic relocation leaves alone
 *   - org.hibernate
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
 */
public record DependencyDescriptor(
        @NotNull List<DependencyEntry> entries,
        @NotNull List<String> repositories,
        boolean communityRepositories,
        @NotNull List<String> relocationExcludes
) {

    /** A descriptor that declares nothing. */
    public static final DependencyDescriptor EMPTY = new DependencyDescriptor(List.of(), List.of(), false, List.of());

    /**
     * Copies the lists so the descriptor cannot change after creation.
     */
    public DependencyDescriptor {
        entries = List.copyOf(entries);
        repositories = List.copyOf(repositories);
        relocationExcludes = List.copyOf(relocationExcludes);
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
