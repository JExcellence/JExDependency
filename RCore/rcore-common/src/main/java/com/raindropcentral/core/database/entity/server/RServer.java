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

    private static final int MIN_SERVER_NAME_LENGTH = 1;
    private static final int MAX_SERVER_NAME_LENGTH = 50;
    
    /**
     * Column mapping for {@code unique_id}. Serves as the public identifier for
     * correlating remote server instances and remains stable for the life of the row.
     */
    @Column(name = "unique_id", unique = true, nullable = false)
    private UUID uniqueId;

    /**
     * Column mapping for {@code server_name}. Updated when administrative
     * configuration changes the friendly display name while respecting length limits.
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

    @Override
    public String toString() {
        return "RServer[id=%d, uuid=%s, name=%s]"
            .formatted(getId(), uniqueId, serverName);
    }
}
