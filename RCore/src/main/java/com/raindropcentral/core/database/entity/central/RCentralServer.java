package com.raindropcentral.core.database.entity.central;

import com.raindropcentral.core.database.entity.player.RPlayer;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "r_central_server")
public class RCentralServer extends AbstractEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "server_uuid", unique = true, nullable = false)
    private UUID serverUuid;

    @Column(name = "owner_minecraft_uuid")
    private String ownerMinecraftUuid;

    @Column(name = "api_key_hash", nullable = true, length = 60)
    private String apiKeyHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "connection_status", nullable = false)
    private ConnectionStatus connectionStatus = ConnectionStatus.DISCONNECTED;

    @Column(name = "last_heartbeat")
    private LocalDateTime lastHeartbeat;

    @Column(name = "current_players")
    private Integer currentPlayers = 0;

    @Column(name = "max_players")
    private Integer maxPlayers = 0;

    @Column(name = "tps")
    private Double tps = 20.0;

    @Column(name = "server_version", length = 50)
    private String serverVersion;

    @Column(name = "plugin_version", length = 20)
    private String pluginVersion;

    @Column(name = "is_public")
    private boolean isPublic = false;

    @Column(name = "share_player_list")
    private boolean sharePlayerList = true;

    @Column(name = "share_metrics")
    private boolean shareMetrics = true;

    @Column(name = "failed_heartbeat_count")
    private int failedHeartbeatCount = 0;

    @Column(name = "first_connected_at")
    private LocalDateTime firstConnectedAt;

    @Column(name = "api_key_displayed_until")
    private LocalDateTime apiKeyDisplayedUntil;

    @ManyToMany(mappedBy = "serversJoined")
    private Set<RPlayer> players = new HashSet<>();

    protected RCentralServer() {}

    public RCentralServer(final @NotNull UUID serverUuid) {
        this.serverUuid = Objects.requireNonNull(serverUuid, "serverUuid cannot be null");
        this.connectionStatus = ConnectionStatus.DISCONNECTED;
    }

    public @NotNull UUID getServerUuid() { return serverUuid; }
    public @Nullable String getOwnerMinecraftUuid() { return ownerMinecraftUuid; }
    public void setOwnerMinecraftUuid(final @Nullable String ownerMinecraftUuid) { this.ownerMinecraftUuid = ownerMinecraftUuid; }
    public @Nullable String getApiKeyHash() { return apiKeyHash; }
    public void setApiKeyHash(final @Nullable String apiKeyHash) { this.apiKeyHash = apiKeyHash; }
    public @NotNull ConnectionStatus getConnectionStatus() { return connectionStatus; }
    public void setConnectionStatus(final @NotNull ConnectionStatus status) { this.connectionStatus = Objects.requireNonNull(status); }
    public @Nullable LocalDateTime getLastHeartbeat() { return lastHeartbeat; }
    public void setLastHeartbeat(final @Nullable LocalDateTime lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }
    public int getCurrentPlayers() { return currentPlayers != null ? currentPlayers : 0; }
    public void setCurrentPlayers(final int currentPlayers) { this.currentPlayers = currentPlayers; }
    public int getMaxPlayers() { return maxPlayers != null ? maxPlayers : 0; }
    public void setMaxPlayers(final int maxPlayers) { this.maxPlayers = maxPlayers; }
    public double getTps() { return tps != null ? tps : 20.0; }
    public void setTps(final double tps) { this.tps = tps; }
    public @Nullable String getServerVersion() { return serverVersion; }
    public void setServerVersion(final @Nullable String serverVersion) { this.serverVersion = serverVersion; }
    public @Nullable String getPluginVersion() { return pluginVersion; }
    public void setPluginVersion(final @Nullable String pluginVersion) { this.pluginVersion = pluginVersion; }
    public boolean isPublic() { return isPublic; }
    public void setPublic(final boolean isPublic) { this.isPublic = isPublic; }
    public boolean isSharePlayerList() { return sharePlayerList; }
    public void setSharePlayerList(final boolean sharePlayerList) { this.sharePlayerList = sharePlayerList; }
    public boolean isShareMetrics() { return shareMetrics; }
    public void setShareMetrics(final boolean shareMetrics) { this.shareMetrics = shareMetrics; }
    public int getFailedHeartbeatCount() { return failedHeartbeatCount; }
    public void setFailedHeartbeatCount(final int count) { this.failedHeartbeatCount = count; }
    public void incrementFailedHeartbeatCount() { this.failedHeartbeatCount++; }
    public void resetFailedHeartbeatCount() { this.failedHeartbeatCount = 0; }
    public @Nullable LocalDateTime getFirstConnectedAt() { return firstConnectedAt; }
    public void setFirstConnectedAt(final @Nullable LocalDateTime firstConnectedAt) { this.firstConnectedAt = firstConnectedAt; }
    public @Nullable LocalDateTime getApiKeyDisplayedUntil() { return apiKeyDisplayedUntil; }
    public void setApiKeyDisplayedUntil(final @Nullable LocalDateTime apiKeyDisplayedUntil) { this.apiKeyDisplayedUntil = apiKeyDisplayedUntil; }
    public boolean isApiKeyDisplayExpired() { return apiKeyDisplayedUntil != null && LocalDateTime.now().isAfter(apiKeyDisplayedUntil); }
    public boolean isConnected() { return connectionStatus == ConnectionStatus.CONNECTED; }
    public @NotNull Set<RPlayer> getPlayers() { return players; }

    public void updateMetrics(final int currentPlayers, final int maxPlayers, final double tps) {
        this.currentPlayers = currentPlayers;
        this.maxPlayers = maxPlayers;
        this.tps = tps;
    }

    @Override
    public String toString() {
        return "RCentralServer[id=%d, uuid=%s, status=%s]".formatted(getId(), serverUuid, connectionStatus);
    }

    public enum ConnectionStatus {
        CONNECTED, DISCONNECTED, ERROR
    }
}
