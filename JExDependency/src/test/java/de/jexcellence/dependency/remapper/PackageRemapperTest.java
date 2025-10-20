package de.jexcellence.dependency.remapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackageRemapperTest {

    @TempDir
    Path tempDir;

    @Test
    void addMappingIgnoresInvalidInputs() throws Exception {
        final PackageRemapper remapper = new PackageRemapper();

        remapper.addMapping("", "com.acme");
        remapper.addMapping("com.example", "");
        remapper.addMapping("com.example.", "com.example");
        remapper.addMapping(" com.example ", " com.example ");

        final Field field = PackageRemapper.class.getDeclaredField("packageMappings");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        final Map<String, String> mappings = (Map<String, String>) field.get(remapper);
        assertTrue(mappings.isEmpty(), "No mappings should be registered for invalid inputs");
    }

    @Test
    void remapRelocatesClassesResourcesAndServicesWhileDroppingSignatures() throws IOException {
        final Path inputJar = tempDir.resolve("input.jar");
        TestJarBuilder.create()
                .withManifestAttribute("Created-By", "Fixture")
                .addClass("com.example.Foo")
                .addClass("com.example.impl.ServiceImpl")
                .addResource("com/example/config.yml", "value=true\n".getBytes(StandardCharsets.UTF_8))
                .addServiceDescriptorWithComment("com.example.Service", "Primary implementation", "com.example.impl.ServiceImpl")
                .addResource("META-INF/extra.txt", "data".getBytes(StandardCharsets.UTF_8))
                .addSignatureFile("SIGNATURE", "SF")
                .addSignatureFile("SIGNATURE", "RSA")
                .build(inputJar);

        final Path outputJar = tempDir.resolve("out/remapped.jar");

        final PackageRemapper remapper = new PackageRemapper();
        remapper.addMapping("com.example", "com.acme.remapped");
        remapper.remap(inputJar, outputJar);

        try (JarFile in = new JarFile(inputJar.toFile());
             JarFile out = new JarFile(outputJar.toFile())) {

            final Manifest inManifest = in.getManifest();
            final Manifest outManifest = out.getManifest();
            assertNotNull(inManifest, "Input manifest should exist");
            assertNotNull(outManifest, "Output manifest should exist");

            final Attributes inAttributes = inManifest.getMainAttributes();
            final Attributes outAttributes = outManifest.getMainAttributes();
            assertEquals(inAttributes.getValue(Attributes.Name.MANIFEST_VERSION),
                    outAttributes.getValue(Attributes.Name.MANIFEST_VERSION));
            assertEquals(inAttributes.getValue("Created-By"), outAttributes.getValue("Created-By"));

            assertNull(out.getJarEntry("com/example/Foo.class"));
            assertNull(out.getJarEntry("com/example/impl/ServiceImpl.class"));

            assertNotNull(out.getJarEntry("com/acme/remapped/Foo.class"));
            assertNotNull(out.getJarEntry("com/acme/remapped/impl/ServiceImpl.class"));
            assertNotNull(out.getJarEntry("com/acme/remapped/config.yml"));

            assertNull(out.getJarEntry("META-INF/SIGNATURE.SF"));
            assertNull(out.getJarEntry("META-INF/SIGNATURE.RSA"));
            assertNotNull(out.getJarEntry("META-INF/extra.txt"));

            final JarEntry serviceEntry = out.getJarEntry("META-INF/services/com.example.Service");
            assertNotNull(serviceEntry, "Service descriptor should exist");
            final String serviceContent;
            try (InputStream is = out.getInputStream(serviceEntry)) {
                serviceContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
            assertTrue(serviceContent.contains("com.acme.remapped.impl.ServiceImpl"));
            assertTrue(serviceContent.startsWith("# Primary implementation"));

            final JarEntry resourceEntry = out.getJarEntry("com/acme/remapped/config.yml");
            assertNotNull(resourceEntry);
            assertArrayEquals(
                    readAll(in, "com/example/config.yml"),
                    readAll(out, "com/acme/remapped/config.yml"));
        }
    }

    @Test
    void remapWithoutMappingsCopiesJarVerbatim() throws IOException {
        final Path sourceJar = tempDir.resolve("source.jar");
        TestJarBuilder.create()
                .addClass("com.example.Sample")
                .addResource("config.yml", "key=value".getBytes(StandardCharsets.UTF_8))
                .addServiceDescriptor("com.example.Service", "com.example.Sample")
                .addSignatureFile("SAMPLE", "SF")
                .build(sourceJar);

        final Path outputJar = tempDir.resolve("copy.jar");

        final PackageRemapper remapper = new PackageRemapper();
        remapper.remap(sourceJar, outputJar);

        try (JarFile in = new JarFile(sourceJar.toFile());
             JarFile out = new JarFile(outputJar.toFile())) {

            assertNotNull(out.getJarEntry("com/example/Sample.class"));
            assertNotNull(out.getJarEntry("config.yml"));

            final Set<String> inputEntries = in.stream()
                    .map(JarEntry::getName)
                    .filter(name -> !name.equals("META-INF/MANIFEST.MF") && !name.equals("META-INF/SAMPLE.SF"))
                    .collect(java.util.stream.Collectors.toSet());
            final Set<String> outputEntries = out.stream().map(JarEntry::getName)
                    .collect(java.util.stream.Collectors.toSet());
            assertTrue(outputEntries.containsAll(inputEntries));

            assertArrayEquals(readAll(in, "com/example/Sample.class"), readAll(out, "com/example/Sample.class"));
            assertArrayEquals(readAll(in, "config.yml"), readAll(out, "config.yml"));

            final JarEntry serviceEntry = out.getJarEntry("META-INF/services/com.example.Service");
            assertNotNull(serviceEntry);
            assertEquals(
                    new String(readAll(in, "META-INF/services/com.example.Service"), StandardCharsets.UTF_8),
                    new String(readAll(out, "META-INF/services/com.example.Service"), StandardCharsets.UTF_8)
            );

            assertNull(out.getJarEntry("META-INF/SAMPLE.SF"));
        }
    }

    private static byte[] readAll(final JarFile jarFile, final String name) throws IOException {
        final JarEntry entry = jarFile.getJarEntry(name);
        assertNotNull(entry, "Entry " + name + " should be present");
        try (InputStream is = jarFile.getInputStream(entry)) {
            return is.readAllBytes();
        }
    }
}
