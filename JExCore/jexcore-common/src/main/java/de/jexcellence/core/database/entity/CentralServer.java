package de.jexcellence.core.database.entity;

import de.jexcellence.jehibernate.entity.base.LongIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A server registered with RaindropCentral. Tracks connection state,
 * heartbeat health, player counts, and sharing preferences.
 */
@Entity
@Table(name = "jexcore_central_server")
public class CentralServer extends LongIdEntity {

    @Column(name = "server_uuid", unique = true, nullable = false)
    private UUID serverUuid;

    @Column(name = "owner_minecraft_uuid")
    private String ownerMinecraftUuid;

    @Column(name = "api_key_hash", length = 60)
    private String apiKeyHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "connection_status", nullable = false)
    private ConnectionStatus connectionStatus = ConnectionStatus.DISCONNECTED;

    @Column(name = "last_heartbeat")
    private LocalDateTime lastHeartbeat;

    @Column(name = "current_players")
    private int currentPlayers;

    @Column(name = "max_players")
    private int maxPlayers;

    @Column(name = "tps")
    private double tps = 20.0;

    @Column(name = "server_version", length = 50)
    private String serverVersion;

    @Column(name = "plugin_version", length = 20)
    private String pluginVersion;

    @Lob
    @Column(name = "allowed_item_codes_json")
    private String allowedItemCodesJson;

    @Column(name = "allowed_item_codes_fetched_at")
    private LocalDateTime allowedItemCodesFetchedAt;

    @Column(name = "is_public")
    private boolean isPublic;

    @Column(name = "share_player_list")
    private boolean sharePlayerList = true;

    @Column(name = "share_metrics")
    private boolean shareMetrics = true;

    @Column(name = "failed_heartbeat_count")
    private int failedHeartbeatCount;

    @Column(name = "first_connected_at")
    private LocalDateTime firstConnectedAt;

    @Column(name = "api_key_displayed_until")
    private LocalDateTime apiKeyDisplayedUntil;

    @ManyToMany(mappedBy = "serversJoined")
    private Set<CorePlayer> players = new HashSet<>();

    protected CentralServer() {
    }

    public CentralServer(@NotNull UUID serverUuid) {
        this.serverUuid = serverUuid;
    }

    public @NotNull UUID getServerUuid() {
        return this.serverUuid;
    }

    public @Nullable String getOwnerMinecraftUuid() {
        return this.ownerMinecraftUuid;
    }

    public void setOwnerMinecraftUuid(@Nullable String ownerMinecraftUuid) {
        this.ownerMinecraftUuid = ownerMinecraftUuid;
    }

    public @Nullable String getApiKeyHash() {
        return this.apiKeyHash;
    }

    public void setApiKeyHash(@Nullable String apiKeyHash) {
        this.apiKeyHash = apiKeyHash;
    }

    public @NotNull ConnectionStatus getConnectionStatus() {
        return this.connectionStatus;
    }

    public void setConnectionStatus(@NotNull ConnectionStatus connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    public @Nullable LocalDateTime getLastHeartbeat() {
        return this.lastHeartbeat;
    }

    public void setLastHeartbeat(@Nullable LocalDateTime lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    public int getCurrentPlayers() {
        return this.currentPlayers;
    }

    public void setCurrentPlayers(int currentPlayers) {
        this.currentPlayers = currentPlayers;
    }

    public int getMaxPlayers() {
        return this.maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public double getTps() {
        return this.tps;
    }

    public void setTps(double tps) {
        this.tps = tps;
    }

    public @Nullable String getServerVersion() {
        return this.serverVersion;
    }

    public void setServerVersion(@Nullable String serverVersion) {
        this.serverVersion = serverVersion;
    }

    public @Nullable String getPluginVersion() {
        return this.pluginVersion;
    }

    public void setPluginVersion(@Nullable String pluginVersion) {
        this.pluginVersion = pluginVersion;
    }

    public @Nullable String getAllowedItemCodesJson() {
        return this.allowedItemCodesJson;
    }

    public void setAllowedItemCodesJson(@Nullable String allowedItemCodesJson) {
        this.allowedItemCodesJson = allowedItemCodesJson;
    }

    public @Nullable LocalDateTime getAllowedItemCodesFetchedAt() {
        return this.allowedItemCodesFetchedAt;
    }

    public void setAllowedItemCodesFetchedAt(@Nullable LocalDateTime allowedItemCodesFetchedAt) {
        this.allowedItemCodesFetchedAt = allowedItemCodesFetchedAt;
    }

    public boolean isPublic() {
        return this.isPublic;
    }

    public void setPublic(boolean publicFlag) {
        this.isPublic = publicFlag;
    }

    public boolean isSharePlayerList() {
        return this.sharePlayerList;
    }

    public void setSharePlayerList(boolean sharePlayerList) {
        this.sharePlayerList = sharePlayerList;
    }

    public boolean isShareMetrics() {
        return this.shareMetrics;
    }

    public void setShareMetrics(boolean shareMetrics) {
        this.shareMetrics = shareMetrics;
    }

    public int getFailedHeartbeatCount() {
        return this.failedHeartbeatCount;
    }

    public void setFailedHeartbeatCount(int failedHeartbeatCount) {
        this.failedHeartbeatCount = failedHeartbeatCount;
    }

    public @Nullable LocalDateTime getFirstConnectedAt() {
        return this.firstConnectedAt;
    }

    public void setFirstConnectedAt(@Nullable LocalDateTime firstConnectedAt) {
        this.firstConnectedAt = firstConnectedAt;
    }

    public @Nullable LocalDateTime getApiKeyDisplayedUntil() {
        return this.apiKeyDisplayedUntil;
    }

    public void setApiKeyDisplayedUntil(@Nullable LocalDateTime apiKeyDisplayedUntil) {
        this.apiKeyDisplayedUntil = apiKeyDisplayedUntil;
    }

    public @NotNull Set<CorePlayer> getPlayers() {
        return this.players;
    }

    @Override
    public String toString() {
        return "CentralServer[" + this.serverUuid + "/" + this.connectionStatus + "]";
    }

    public enum ConnectionStatus {
        CONNECTED, DISCONNECTED, ERROR
    }
}
