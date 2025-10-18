/**
 * Database bootstrap helpers and Hibernate integration glue for the shared platform.
 *
 * <h2>Resource provisioning</h2>
 * <p>{@link com.raindropcentral.rplatform.RPlatform#initializeDatabaseResources()} creates the on-disk
 * {@code database/hibernate.properties} file by copying the bundled resource on first boot before wiring
 * the shared {@link jakarta.persistence.EntityManagerFactory} through {@link de.jexcellence.hibernate.JEHibernate}.【F:RPlatform/src/main/java/com/raindropcentral/rplatform/RPlatform.java†L18-L190】
 * The method runs inside the asynchronous platform bootstrap, guaranteeing that repositories created during
 * the later lifecycle stages receive a ready factory.</p>
 *
 * <h2>Schema and resource synchronisation</h2>
 * <p>Because {@code JavaPlugin#saveResource(String, boolean)} is invoked with {@code replace=false}, updated
 * {@code hibernate.properties} files are not overwritten automatically. When shipping schema tweaks or new
 * connection properties, bump the bundled template and instruct operators to delete the existing
 * configuration so the resource can be re-copied, or ship a manual migration task that rewrites the file at
 * runtime. After changing mappings, run the RDQ and RCore integration tests against both free and premium
 * editions to confirm the shared EntityManager can still open sessions.</p>
 *
 * <h2>Converter coordination</h2>
 * <p>The attribute converters housed in {@link com.raindropcentral.rplatform.database.converter} provide the
 * Bukkit-to-JPA translation that allows statistics, perk payloads, and quest metadata to persist complex
 * objects such as {@link org.bukkit.Location} and {@link org.bukkit.inventory.ItemStack}. Keep these
 * converters updated alongside schema changes so database rows remain backward compatible.</p>
 */
package com.raindropcentral.rplatform.database;
