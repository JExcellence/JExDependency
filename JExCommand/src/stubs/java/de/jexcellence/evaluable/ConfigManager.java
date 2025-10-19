package de.jexcellence.evaluable;

import de.jexcellence.configmapper.ConfigMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Simplified configuration manager used only for test scenarios. It mirrors the
 * public API required by {@link com.raindropcentral.commands.CommandFactory}
 * without depending on the proprietary implementation bundled with the real
 * plugin.
 */
public class ConfigManager {

    private final Object plugin;
    private final String folder;

    public ConfigManager(Object plugin, String folder) {
        this.plugin = plugin;
        this.folder = folder;
    }

    public ConfigMapper loadConfig(String fileName) throws IOException {
        String normalized = fileName.toLowerCase(Locale.ROOT);
        Path target = resolveDataFolder().toPath().resolve(this.folder).resolve(normalized);
        Files.createDirectories(target.getParent());
        if (Files.notExists(target)) {
            try (InputStream stream = resolveResource(this.folder + "/" + normalized)) {
                if (stream != null) {
                    Files.copy(stream, target);
                } else {
                    Files.createFile(target);
                }
            }
        }
        return new ConfigMapper();
    }

    public Object getPlugin() {
        return this.plugin;
    }

    public String getFolder() {
        return this.folder;
    }

    private File resolveDataFolder() {
        try {
            Method method = this.plugin.getClass().getMethod("getDataFolder");
            Object result = method.invoke(this.plugin);
            if (result instanceof File file) {
                return file;
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to resolve plugin data folder", exception);
        }
        throw new IllegalStateException("Plugin data folder unavailable");
    }

    private InputStream resolveResource(String name) {
        try {
            Method method = this.plugin.getClass().getMethod("getResource", String.class);
            Object result = method.invoke(this.plugin, name);
            if (result instanceof InputStream stream) {
                return stream;
            }
        } catch (Exception ignored) {
            // Fallback to class loader below
        }
        return this.plugin.getClass().getClassLoader().getResourceAsStream(name);
    }
}
