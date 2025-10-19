package com.raindropcentral.core.database.entity.server;

import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a persisted server descriptor stored in the {@code r_server} table.
 * <p>
 * Each row tracks a unique Paper server instance by {@link UUID} and provides a
 * human-readable server name. Player inventory snapshots and other aggregates
 * reference this entity through foreign keys to partition data by server.
 * </p>
 * <p>
 * Provisioning, renaming, and deleting server descriptors should be accompanied by
 * {@link com.raindropcentral.rplatform.logging.CentralLogger CentralLogger} events. Emit info logs
 * when a new server entry is created or when a rename is requested, include debug logs when a cache
 * miss forces a refresh from the repository, and escalate failures (duplicate UUID, constraint
 * violations) to error logs that include the UUID and friendly name so cross-cluster routing issues
 * can be diagnosed.
 * </p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@Entity
@Table(name = "r_server")
public class RServer extends AbstractEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Minimum inclusive length for {@code server_name} values after trimming whitespace.
     */
    private static final int MIN_SERVER_NAME_LENGTH = 1;

    /**
     * Maximum inclusive length permitted by the {@code server_name} column definition.
     */
    private static final int MAX_SERVER_NAME_LENGTH = 50;
    
    /**
     * Column mapping for {@code unique_id}. Serves as the public identifier for correlating remote
     * server instances and remains stable for the life of the row. Declared {@code nullable = false}
     * and {@code unique = true} to mirror the persistence constraint enforced by the schema.
     */
    @Column(name = "unique_id", unique = true, nullable = false)
    private UUID uniqueId;

    /**
     * Column mapping for {@code server_name}. Updated when administrative configuration changes the
     * friendly display name while respecting length limits. Persisted as {@code nullable = false}
     * with a maximum column {@code length} of {@value #MAX_SERVER_NAME_LENGTH} characters.
     */
    @Column(name = "server_name", nullable = false, length = MAX_SERVER_NAME_LENGTH)
    private String serverName;

    /**
     * Protected no-args constructor for Hibernate use only.
     */
    protected RServer() {}

    /**
     * Creates a new server descriptor for persistence.
     *
     * @param uniqueId   stable server UUID assigned by the platform
     * @param serverName friendly name presented to players and operators
     */
    public RServer(final @NotNull UUID uniqueId, final @NotNull String serverName) {
        this.uniqueId = Objects.requireNonNull(uniqueId, "uniqueId cannot be null");
        this.serverName = validateServerName(serverName);
    }

    /**
     * Retrieves the immutable UUID backing the {@code unique_id} column.
     *
     * @return globally unique server identifier
     */
    public @NotNull UUID getUniqueId() {
        return this.uniqueId;
    }

    /**
     * Provides the current display name stored in {@code server_name}.
     *
     * @return persisted server display name
     */
    public @NotNull String getServerName() {
        return this.serverName;
    }

    /**
     * Updates the stored server display name while validating configured length bounds.
     *
     * @param newName replacement server name to persist
     */
    public void updateServerName(final @NotNull String newName) {
        this.serverName = validateServerName(newName);
    }

    /**
     * Normalizes and validates candidate server names before persistence or assignment.
     *
     * @param name raw server name input
     * @return trimmed server name compliant with configured bounds
     * @throws IllegalArgumentException if the normalized value violates length constraints
     */
    private static String validateServerName(final @NotNull String name) {
        Objects.requireNonNull(name, "serverName cannot be null");

        final String trimmed = name.trim();
        if (trimmed.length() < MIN_SERVER_NAME_LENGTH || trimmed.length() > MAX_SERVER_NAME_LENGTH) {
            throw new IllegalArgumentException(
                "Server name must be between %d and %d characters, got: %d"
                    .formatted(MIN_SERVER_NAME_LENGTH, MAX_SERVER_NAME_LENGTH, trimmed.length())
            );
        }
        
        return trimmed;
    }

    /**
     * Provides a concise descriptor useful for operator logs and debugging output.
     *
     * @return formatted identifier containing the primary key, UUID, and display name
     */
    @Override
    public String toString() {
        return "RServer[id=%d, uuid=%s, name=%s]"
            .formatted(getId(), uniqueId, serverName);
    }
}
