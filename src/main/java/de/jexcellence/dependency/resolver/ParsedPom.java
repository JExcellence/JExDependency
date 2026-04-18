package de.jexcellence.dependency.resolver;

import de.jexcellence.dependency.model.DependencyCoordinate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Immutable snapshot of the dependency-resolution–relevant content of a Maven POM file.
 *
 * <p>After the {@link TransitiveDependencyResolver} walks the parent POM chain, it produces
 * a merged instance where {@link #properties()} and {@link #dependencyManagement()} contain
 * contributions from every ancestor (child values take precedence over parent values).
 *
 * @param groupId             effective {@code groupId} (inherited from parent when absent in the POM)
 * @param artifactId          {@code artifactId} of this artifact
 * @param version             effective {@code version} (inherited from parent when absent in the POM)
 * @param parent              parent artifact coordinates, or {@code null} for root POMs
 * @param properties          merged property map including self-referential entries such as
 *                            {@code project.version} and {@code project.groupId}
 * @param dependencyManagement merged {@code <dependencyManagement>} entries used for version lookup
 * @param dependencies        direct {@code <dependencies>} entries declared in this POM
 *                            (not the parent chain — parents are merged into {@code dependencyManagement})
 */
public record ParsedPom(
        @NotNull String groupId,
        @NotNull String artifactId,
        @NotNull String version,
        @Nullable DependencyCoordinate parent,
        @NotNull Map<String, String> properties,
        @NotNull List<PomDependency> dependencyManagement,
        @NotNull List<PomDependency> dependencies
) {}
