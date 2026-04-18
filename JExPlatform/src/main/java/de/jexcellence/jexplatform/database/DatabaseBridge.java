package de.jexcellence.jexplatform.database;

import de.jexcellence.jehibernate.core.JEHibernate;
import de.jexcellence.jexplatform.logging.JExLogger;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * Thin bridge to JEHibernate for database initialization from plugin configuration.
 *
 * <p>Copies a bundled {@code hibernate.properties} to the plugin's data folder
 * (if absent), then delegates to {@link JEHibernate} for entity manager creation:
 *
 * <pre>{@code
 * var emf = DatabaseBridge.initialize(plugin, log).join();
 * var em = emf.createEntityManager();
 * }</pre>
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class DatabaseBridge {

    private static final String PROPERTIES_RESOURCE = "hibernate.properties";
    private static final String DATABASE_DIR = "database";

    private DatabaseBridge() {
        // Utility class
    }

    /**
     * Initializes the database asynchronously by provisioning the configuration
     * file and creating an {@link EntityManagerFactory}.
     *
     * <p>The bundled {@code hibernate.properties} is copied to
     * {@code <dataFolder>/database/hibernate.properties} only if the file does
     * not already exist, allowing operators to customise database settings.
     *
     * @param plugin the owning plugin (provides data folder and resources)
     * @param log    logger for diagnostics
     * @return future completing with the entity manager factory
     */
    public static @NotNull CompletableFuture<EntityManagerFactory> initialize(
            @NotNull JavaPlugin plugin,
            @NotNull JExLogger log
    ) {
        return CompletableFuture.supplyAsync(() -> {
            var dbDir = plugin.getDataFolder().toPath().resolve(DATABASE_DIR);
            var propsFile = dbDir.resolve(PROPERTIES_RESOURCE);

            try {
                Files.createDirectories(dbDir);
                if (Files.notExists(propsFile)) {
                    plugin.saveResource(DATABASE_DIR + "/" + PROPERTIES_RESOURCE, false);
                    log.info("Created default {}", propsFile);
                }
            } catch (IOException e) {
                throw new IllegalStateException("Failed to provision database directory", e);
            }

            log.info("Initializing JEHibernate from {}", propsFile);
            
            // Load properties from file
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(propsFile.toFile())) {
                props.load(fis);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load hibernate.properties", e);
            }
            
            // Convert Properties to Map<String, String>
            Map<String, String> properties = new HashMap<>();
            for (String key : props.stringPropertyNames()) {
                properties.put(key, props.getProperty(key));
            }
            
            // Create EntityManagerFactory using Jakarta Persistence
            var emf = Persistence.createEntityManagerFactory("default-pu", properties);
            log.info("Database ready");
            return emf;
        });
    }

    /**
     * Initializes the database synchronously.
     *
     * @param plugin the owning plugin
     * @param log    logger for diagnostics
     * @return the entity manager factory
     * @see #initialize(JavaPlugin, JExLogger)
     */
    public static @NotNull EntityManagerFactory initializeSync(
            @NotNull JavaPlugin plugin,
            @NotNull JExLogger log
    ) {
        return initialize(plugin, log).join();
    }
}
