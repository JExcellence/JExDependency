package de.jexcellence.dependency.remapper;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

final class TestJarBuilder {

    private final Map<String, byte[]> entries = new LinkedHashMap<>();
    private final Manifest manifest = new Manifest();

    private TestJarBuilder() {
        final Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.putValue("Created-By", "PackageRemapperTest");
    }

    static TestJarBuilder create() {
        return new TestJarBuilder();
    }

    TestJarBuilder withManifestAttribute(final String name, final String value) {
        manifest.getMainAttributes().putValue(name, value);
        return this;
    }

    TestJarBuilder addClass(final String fqcn) {
        final String entryName = fqcn.replace('.', '/') + ".class";
        entries.put(entryName, generateClassBytes(fqcn));
        return this;
    }

    TestJarBuilder addResource(final String name, final byte[] data) {
        entries.put(name, data);
        return this;
    }

    TestJarBuilder addServiceDescriptor(final String service, final String... implementations) {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < implementations.length; i++) {
            if (i > 0) {
                builder.append('\n');
            }
            builder.append(implementations[i]);
        }
        entries.put("META-INF/services/" + service, builder.toString().getBytes(StandardCharsets.UTF_8));
        return this;
    }

    TestJarBuilder addServiceDescriptorWithComment(final String service, final String headerComment, final String... implementations) {
        final StringBuilder builder = new StringBuilder();
        builder.append("# ").append(headerComment).append('\n');
        for (int i = 0; i < implementations.length; i++) {
            if (i > 0) {
                builder.append('\n');
            }
            builder.append(implementations[i]);
        }
        entries.put("META-INF/services/" + service, builder.toString().getBytes(StandardCharsets.UTF_8));
        return this;
    }

    TestJarBuilder addSignatureFile(final String name, final String extension) {
        final String entryName = "META-INF/" + name + "." + extension;
        entries.put(entryName, (name + '.' + extension).getBytes(StandardCharsets.UTF_8));
        return this;
    }

    Path build(final Path targetJar) throws IOException {
        final Path parent = targetJar.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (OutputStream os = Files.newOutputStream(targetJar);
             JarOutputStream jos = new JarOutputStream(os, manifest)) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                final JarEntry jarEntry = new JarEntry(entry.getKey());
                jos.putNextEntry(jarEntry);
                jos.write(entry.getValue());
                jos.closeEntry();
            }
            jos.flush();
        }
        return targetJar;
    }

    private static byte[] generateClassBytes(final String fqcn) {
        final String internalName = fqcn.replace('.', '/');
        final ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, internalName, null, "java/lang/Object", null);
        writer.visitEnd();
        return writer.toByteArray();
    }
}
