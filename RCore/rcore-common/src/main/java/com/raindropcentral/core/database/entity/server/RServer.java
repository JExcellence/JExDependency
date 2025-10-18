package com.raindropcentral.core.database.entity.server;

import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "r_server")
public class RServer extends AbstractEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final int MIN_SERVER_NAME_LENGTH = 1;
    private static final int MAX_SERVER_NAME_LENGTH = 50;
    
    @Column(name = "unique_id", unique = true, nullable = false)
    private UUID uniqueId;
    
    @Column(name = "server_name", nullable = false, length = MAX_SERVER_NAME_LENGTH)
    private String serverName;

    protected RServer() {}

    public RServer(final @NotNull UUID uniqueId, final @NotNull String serverName) {
        this.uniqueId = Objects.requireNonNull(uniqueId, "uniqueId cannot be null");
        this.serverName = validateServerName(serverName);
    }

    public @NotNull UUID getUniqueId() {
        return this.uniqueId;
    }

    public @NotNull String getServerName() {
        return this.serverName;
    }

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
