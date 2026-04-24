package de.jexcellence.dependency.resolver;

import de.jexcellence.dependency.model.DependencyCoordinate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Parses a Maven POM XML stream into a {@link ParsedPom} using the JDK's built-in
 * {@link DocumentBuilder} — no external XML library is required.
 *
 * <p>Only the fields relevant to runtime transitive dependency resolution are extracted:
 * {@code <parent>}, {@code <properties>}, {@code <dependencyManagement>}, and
 * {@code <dependencies>}.  Profile-specific sections, build plugins, and reporting
 * configuration are ignored.
 *
 * <p>Instances are stateless and thread-safe after construction; a single instance can be
 * shared across concurrent resolution tasks.
 */
public class PomParser {

    private static final Logger LOGGER = Logger.getLogger("JExDependency");

    private static final String ELEMENT_GROUP_ID = "groupId";
    private static final String ELEMENT_ARTIFACT_ID = "artifactId";
    private static final String ELEMENT_VERSION = "version";
    private static final String ELEMENT_DEPENDENCIES = "dependencies";
    private static final String ELEMENT_DEPENDENCY = "dependency";
    private static final String UNKNOWN_VALUE = "unknown";

    /**
     * Parses the supplied POM input stream and returns a {@link ParsedPom}, or {@code null}
     * if the stream cannot be parsed (malformed XML, I/O error, etc.).
     *
     * @param stream POM XML input stream; the caller is responsible for closing it
     * @return parsed POM or {@code null} on error
     */
    public @Nullable ParsedPom parse(@NotNull final InputStream stream) {
        try {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setValidating(false);
            // Disable external entity loading — we only need to parse local/downloaded XML
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            final DocumentBuilder builder = factory.newDocumentBuilder();
            // Suppress the default error handler which writes to stderr
            builder.setErrorHandler(null);

            final Document doc = builder.parse(stream);
            doc.getDocumentElement().normalize();

            final Element root = doc.getDocumentElement();

            // --- Parent ---
            final DependencyCoordinate parent = parseParent(root);

            // --- Own coordinates (groupId / version may be inherited from parent) ---
            String groupId   = directChildText(root, ELEMENT_GROUP_ID);
            String artifactId = directChildText(root, ELEMENT_ARTIFACT_ID);
            String version   = directChildText(root, ELEMENT_VERSION);

            if (groupId   == null && parent != null) groupId   = parent.groupId();
            if (version   == null && parent != null) version   = parent.version();
            if (groupId   == null) groupId   = UNKNOWN_VALUE;
            if (artifactId == null) artifactId = UNKNOWN_VALUE;
            if (version   == null) version   = UNKNOWN_VALUE;

            // --- Properties (seed with self-referential entries) ---
            final Map<String, String> properties = parseProperties(root);
            properties.put("project.groupId",          groupId);
            properties.put("project.artifactId",       artifactId);
            properties.put("project.version",          version);
            // ${version} is a deprecated alias for ${project.version}, still widely used
            properties.putIfAbsent(ELEMENT_VERSION,    version);
            if (parent != null) {
                properties.put("project.parent.version", parent.version());
                properties.put("project.parent.groupId", parent.groupId());
            }

            // --- Dependency management & direct dependencies ---
            final List<PomDependency> depMgmt = parseDependencyManagement(root);
            final List<PomDependency> deps     = parseDependencies(root);

            return new ParsedPom(groupId, artifactId, version, parent, properties, depMgmt, deps);

        } catch (final Exception exception) {
            LOGGER.log(Level.FINE, "Failed to parse POM", exception);
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Section parsers
    // -------------------------------------------------------------------------

    private @Nullable DependencyCoordinate parseParent(@NotNull final Element root) {
        final Element parentEl = directChildElement(root, "parent");
        if (parentEl == null) return null;

        final String groupId   = directChildText(parentEl, ELEMENT_GROUP_ID);
        final String artifactId = directChildText(parentEl, ELEMENT_ARTIFACT_ID);
        final String version   = directChildText(parentEl, ELEMENT_VERSION);

        if (groupId == null || artifactId == null || version == null) return null;
        return new DependencyCoordinate(groupId, artifactId, version);
    }

    private @NotNull Map<String, String> parseProperties(@NotNull final Element root) {
        final Map<String, String> properties = new HashMap<>();
        final Element propertiesEl = directChildElement(root, "properties");
        if (propertiesEl == null) return properties;

        final NodeList children = propertiesEl.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node node = children.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) continue;
            final String key   = localName(node);
            final String value = node.getTextContent().trim();
            if (!key.isEmpty() && !value.isEmpty()) {
                properties.put(key, value);
            }
        }
        return properties;
    }

    private @NotNull List<PomDependency> parseDependencyManagement(@NotNull final Element root) {
        final Element dmEl = directChildElement(root, "dependencyManagement");
        if (dmEl == null) return List.of();

        final Element depsEl = directChildElement(dmEl, ELEMENT_DEPENDENCIES);
        if (depsEl == null) return List.of();

        return parseDependencyList(depsEl);
    }

    private @NotNull List<PomDependency> parseDependencies(@NotNull final Element root) {
        final Element depsEl = directChildElement(root, ELEMENT_DEPENDENCIES);
        if (depsEl == null) return List.of();
        return parseDependencyList(depsEl);
    }

    private @NotNull List<PomDependency> parseDependencyList(@NotNull final Element depsEl) {
        final List<PomDependency> result = new ArrayList<>();
        final NodeList depNodes = depsEl.getChildNodes();

        for (int i = 0; i < depNodes.getLength(); i++) {
            final Node node = depNodes.item(i);
            final PomDependency parsed = parseDependencyNode(node);
            if (parsed != null) {
                result.add(parsed);
            }
        }

        return result;
    }

    /**
     * Parses a single {@code <dependency>} DOM node into a {@link PomDependency}, or returns {@code null} when the
     * node should be skipped (non-element nodes, non-dependency elements, incomplete data, or BOM imports).
     */
    private @Nullable PomDependency parseDependencyNode(@NotNull final Node node) {
        // Only direct <dependency> children - getElementsByTagName would recurse too deep
        if (node.getNodeType() != Node.ELEMENT_NODE || !ELEMENT_DEPENDENCY.equals(localName(node))) {
            return null;
        }

        final Element depEl = (Element) node;

        final String groupId    = directChildText(depEl, ELEMENT_GROUP_ID);
        final String artifactId = directChildText(depEl, ELEMENT_ARTIFACT_ID);
        final String version    = directChildText(depEl, ELEMENT_VERSION);   // may be null
        final String scopeRaw   = directChildText(depEl, "scope");
        final String optionalRaw = directChildText(depEl, "optional");
        final String typeRaw    = directChildText(depEl, "type");

        if (groupId == null || artifactId == null) {
            return null;
        }

        // Skip POM-type entries in dependencyManagement (BOM imports) - following each BOM recursively would
        // make the resolver far more complex and is not needed for the common library-dependency use case.
        if ("pom".equalsIgnoreCase(typeRaw) && PomDependency.SCOPE_IMPORT.equals(scopeRaw)) {
            return null;
        }

        final String scope = scopeRaw != null ? scopeRaw : PomDependency.SCOPE_COMPILE;
        final boolean optional = "true".equalsIgnoreCase(optionalRaw);
        final Set<String> exclusions = parseExclusions(depEl);

        return new PomDependency(groupId, artifactId, version, scope, optional, exclusions);
    }

    private @NotNull Set<String> parseExclusions(@NotNull final Element depEl) {
        final Set<String> exclusions = new HashSet<>();
        final Element exclusionsEl = directChildElement(depEl, "exclusions");
        if (exclusionsEl == null) {
            return exclusions;
        }

        final NodeList excNodes = exclusionsEl.getChildNodes();
        for (int j = 0; j < excNodes.getLength(); j++) {
            final Node excNode = excNodes.item(j);
            if (excNode.getNodeType() != Node.ELEMENT_NODE || !"exclusion".equals(localName(excNode))) {
                continue;
            }
            final Element excEl = (Element) excNode;
            final String excGroup    = directChildText(excEl, ELEMENT_GROUP_ID);
            final String excArtifact = directChildText(excEl, ELEMENT_ARTIFACT_ID);
            if (excGroup != null && excArtifact != null) {
                exclusions.add(excGroup + ":" + excArtifact);
            }
        }
        return exclusions;
    }

    // -------------------------------------------------------------------------
    // XML helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the first direct child element of {@code parent} with the given local name,
     * or {@code null} when none exists.  Only examines immediate children (depth = 1).
     */
    private @Nullable Element directChildElement(@NotNull final Element parent, @NotNull final String tagName) {
        final NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && tagName.equals(localName(node))) {
                return (Element) node;
            }
        }
        return null;
    }

    /**
     * Returns the trimmed text content of the first direct child element with the given local
     * name, or {@code null} when no such element exists or its text is blank.
     */
    private @Nullable String directChildText(@NotNull final Element parent, @NotNull final String tagName) {
        final Element child = directChildElement(parent, tagName);
        if (child == null) return null;
        final String text = child.getTextContent().trim();
        return text.isEmpty() ? null : text;
    }

    /**
     * Returns the local name of a DOM node, falling back to {@link Node#getNodeName()} for
     * parsers that do not set a local name (non-namespace-aware mode).
     */
    private @NotNull String localName(@NotNull final Node node) {
        final String local = node.getLocalName();
        return local != null ? local : node.getNodeName();
    }
}
