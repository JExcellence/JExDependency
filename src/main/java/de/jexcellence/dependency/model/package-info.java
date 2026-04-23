/**
 * Data Transfer Objects that represent dependency coordinates, download results, and processing summaries.
 * for the JExDependency runtime loader.
 *
 * <p>The {@link de.jexcellence.dependency.loader.YamlDependencyLoader} parses {@code dependencies.yml}
 * resources into {@link de.jexcellence.dependency.model.DependencyCoordinate} instances which are then fed
 * into the {@link de.jexcellence.dependency.manager.DependencyManager} and, when remapping is enabled, the
 * {@link de.jexcellence.dependency.remapper.RemappingDependencyManager}. These managers cooperate with the
 * repository abstractions under {@code de.jexcellence.dependency.repository} to calculate remote URLs and
 * download locations before invoking the injector and remapper pipelines.</p>
 *
 * <ul>
 *     <li>{@link de.jexcellence.dependency.model.Dependency} &mdash; canonical representation of a dependency
 *     definition, including optional classifier and repository override metadata. Use this when schema entries
 *     need to capture the repository host alongside the GAV triple before handing the data to repository
 *     strategies.</li>
 *     <li>{@link de.jexcellence.dependency.model.DependencyCoordinate} &mdash; lightweight coordinate used by the
 *     managers, downloaders, and repositories to derive file names and repository paths. Both managers
 *     populate download queues exclusively with these instances.</li>
 *     <li>{@link de.jexcellence.dependency.model.DownloadResult} &mdash; outcome of a single download attempt,
 *     allowing managers to log failures with the originating coordinate and optional error message while passing
 *     successfully resolved files to the injector layer.</li>
 *     <li>{@link de.jexcellence.dependency.model.ProcessingResult} &mdash; aggregate execution summary returned by
 *     the managers to calling code such as {@link de.jexcellence.dependency.JEDependency}. It combines the
 *     success list, failure details, and elapsed time so plugin bootstrap code can react appropriately.</li>
 * </ul>
 *
 * <h2>Serialization and YAML alignment</h2>
 * <p>YAML configuration currently serializes dependencies as a simple list of quoted strings under a
 * {@code dependencies:} key, for example {@code "group:artifact:version"} or
 * {@code "group:artifact:version:classifier"}. The loader forwards those strings directly to
 * {@link de.jexcellence.dependency.model.DependencyCoordinate#parse(String)}. Maintainers extending the YAML
 * schema must keep the colon-delimited order ({@code groupId:artifactId:version[:classifier]}) in sync with the
 * parser and update it alongside any additional fields.</p>
 *
 * <p>If YAML evolves to emit objects instead of strings, prefer field names that mirror the accessor names in
 * {@link de.jexcellence.dependency.model.Dependency} so that object mappers (e.g., SnakeYAML) can hydrate the
 * record without adapters. When introducing repository hints, ensure the consuming repository implementations
 * interpret {@link de.jexcellence.dependency.model.Dependency#repository()} consistently and fall back to the
 * default repository when {@code null}.</p>
 */
package de.jexcellence.dependency.model;
