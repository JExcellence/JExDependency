/**
 * Contains utilities that manage Java module boundaries so relocated
 * dependencies remain accessible after remapping.
 * <p>
 * {@link de.jexcellence.dependency.module.Deencapsulation} opens every package
 * in the plugin's module layer before
 * {@link de.jexcellence.dependency.remapper.RemappingDependencyManager}
 * injects rewritten jars. The class tracks a map of opened {@link Module}
 * instances to their package names, which effectively records the relocation
 * metadata required to close those openings once classpath injection
 * completes. This prevents relocated classes and resources from being blocked
 * by strong encapsulation.
 * <p>
 * The helper relies on JDK internals such as
 * {@link sun.reflect.ReflectionFactory} to synthesize privileged
 * {@link java.lang.invoke.MethodHandles.Lookup} instances. When a JVM denies
 * deep reflection, the utility logs warnings yet allows dependency loading to
 * continue, ensuring runtime failures degrade gracefully rather than stopping
 * plugin startup.
 */
package de.jexcellence.dependency.module;
