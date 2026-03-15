package com.raindropcentral.core.database.entity.central;

import com.raindropcentral.core.database.entity.player.RPlayer;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Represents the type API type.
 */
/**
 * Represents the RCentralServer API type.
 */
@Entity
@Table(name = "r_central_server")
public class RCentralServer extends BaseEntity {

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

    /**
     * Gets serverUuid.
     */
    public @NotNull UUID getServerUuid() { return serverUuid; }
    /**
     * Gets ownerMinecraftUuid.
     */
    public @Nullable String getOwnerMinecraftUuid() { return ownerMinecraftUuid; }
    /**
     * Sets ownerMinecraftUuid.
     */
    public void setOwnerMinecraftUuid(final @Nullable String ownerMinecraftUuid) { this.ownerMinecraftUuid = ownerMinecraftUuid; }
    /**
     * Gets apiKeyHash.
     */
    public @Nullable String getApiKeyHash() { return apiKeyHash; }
    /**
     * Sets apiKeyHash.
     */
    public void setApiKeyHash(final @Nullable String apiKeyHash) { this.apiKeyHash = apiKeyHash; }
    /**
     * Gets connectionStatus.
     */
    public @NotNull ConnectionStatus getConnectionStatus() { return connectionStatus; }
    /**
     * Sets connectionStatus.
     */
    public void setConnectionStatus(final @NotNull ConnectionStatus status) { this.connectionStatus = Objects.requireNonNull(status); }
    /**
     * Gets lastHeartbeat.
     */
    public @Nullable LocalDateTime getLastHeartbeat() { return lastHeartbeat; }
    /**
     * Sets lastHeartbeat.
     */
    public void setLastHeartbeat(final @Nullable LocalDateTime lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }
    /**
     * Gets currentPlayers.
     */
    public int getCurrentPlayers() { return currentPlayers != null ? currentPlayers : 0; }
    public void setCurrentPlayers(final int currentPlayers) { this.currentPlayers = currentPlayers; }
    /**
     * Gets maxPlayers.
     */
    public int getMaxPlayers() { return maxPlayers != null ? maxPlayers : 0; }
    /**
     * Sets maxPlayers.
     */
    public void setMaxPlayers(final int maxPlayers) { this.maxPlayers = maxPlayers; }
    /**
     * Gets tps.
     */
    public double getTps() { return tps != null ? tps : 20.0; }
    /**
     * Sets tps.
     */
    public void setTps(final double tps) { this.tps = tps; }
    /**
     * Gets serverVersion.
     */
    public @Nullable String getServerVersion() { return serverVersion; }
    /**
     * Sets serverVersion.
     */
    public void setServerVersion(final @Nullable String serverVersion) { this.serverVersion = serverVersion; }
    /**
     * Gets pluginVersion.
     */
    public @Nullable String getPluginVersion() { return pluginVersion; }
    /**
     * Sets pluginVersion.
     */
    public void setPluginVersion(final @Nullable String pluginVersion) { this.pluginVersion = pluginVersion; }
    /**
     * Returns whether public.
     */
    public boolean isPublic() { return isPublic; }
    /**
     * Sets public.
     */
    public void setPublic(final boolean isPublic) { this.isPublic = isPublic; }
    /**
     * Returns whether sharePlayerList.
     */
    public boolean isSharePlayerList() { return sharePlayerList; }
    /**
     * Sets sharePlayerList.
     */
    public void setSharePlayerList(final boolean sharePlayerList) { this.sharePlayerList = sharePlayerList; }
    /**
     * Returns whether shareMetrics.
     */
    public boolean isShareMetrics() { return shareMetrics; }
    /**
     * Sets shareMetrics.
     */
    public void setShareMetrics(final boolean shareMetrics) { this.shareMetrics = shareMetrics; }
    /**
     * Gets failedHeartbeatCount.
     */
    public int getFailedHeartbeatCount() { return failedHeartbeatCount; }
    /**
     * Sets failedHeartbeatCount.
     */
    public void setFailedHeartbeatCount(final int count) { this.failedHeartbeatCount = count; }
    public void incrementFailedHeartbeatCount() { this.failedHeartbeatCount++; }
    public void resetFailedHeartbeatCount() { this.failedHeartbeatCount = 0; }
    /**
     * Gets firstConnectedAt.
     */
    public @Nullable LocalDateTime getFirstConnectedAt() { return firstConnectedAt; }
    /**
     * Sets firstConnectedAt.
     */
    public void setFirstConnectedAt(final @Nullable LocalDateTime firstConnectedAt) { this.firstConnectedAt = firstConnectedAt; }
    /**
     * Gets apiKeyDisplayedUntil.
     */
    public @Nullable LocalDateTime getApiKeyDisplayedUntil() { return apiKeyDisplayedUntil; }
    /**
     * Sets apiKeyDisplayedUntil.
     */
    public void setApiKeyDisplayedUntil(final @Nullable LocalDateTime apiKeyDisplayedUntil) { this.apiKeyDisplayedUntil = apiKeyDisplayedUntil; }
    /**
     * Returns whether apiKeyDisplayExpired.
     */
    public boolean isApiKeyDisplayExpired() { return apiKeyDisplayedUntil != null && LocalDateTime.now().isAfter(apiKeyDisplayedUntil); }
    /**
     * Returns whether connected.
     */
    public boolean isConnected() { return connectionStatus == ConnectionStatus.CONNECTED; }
    /**
     * Gets players.
     */
    public @NotNull Set<RPlayer> getPlayers() { return players; }

    /**
     * Performs updateMetrics.
     */
    public void updateMetrics(final int currentPlayers, final int maxPlayers, final double tps) {
        this.currentPlayers = currentPlayers;
        this.maxPlayers = maxPlayers;
        this.tps = tps;
    }

    @Override
    public String toString() {
        return "RCentralServer[id=%d, uuid=%s, status=%s]".formatted(getId(), serverUuid, connectionStatus);
    }

    /**
     * Represents the ConnectionStatus API type.
     */
    public enum ConnectionStatus {
        CONNECTED, DISCONNECTED, ERROR
    }
}
