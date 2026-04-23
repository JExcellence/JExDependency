package de.jexcellence.core.service.central;

import de.jexcellence.core.database.entity.CentralServer;
import de.jexcellence.core.service.CentralServerService;
import de.jexcellence.jexplatform.logging.JExLogger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

/**
 * Manages this server's registration with the RaindropCentral backend and
 * owns the periodic heartbeat loop. Pluggable HTTP client lands later — for
 * now heartbeats update the local database row and log.
 */
public class CentralService {

    private final JavaPlugin plugin;
    private final CentralServerService servers;
    private final JExLogger logger;
    private final ServerContext context;

    private HeartbeatScheduler heartbeat;
    private Long registeredId;
    private volatile boolean connected;

    public CentralService(
            @NotNull JavaPlugin plugin,
            @NotNull CentralServerService servers,
            @NotNull JExLogger logger,
            @NotNull ServerContext context
    ) {
        this.plugin = plugin;
        this.servers = servers;
        this.logger = logger;
        this.context = context;
    }

    public void start() {
        this.servers.register(this.context.serverUuid())
                .thenAccept(row -> {
                    if (row == null) return;
                    row.setServerVersion(this.context.serverVersion());
                    row.setPluginVersion(this.context.pluginVersion());
                    row.setConnectionStatus(CentralServer.ConnectionStatus.CONNECTED);
                    row.setLastHeartbeat(LocalDateTime.now());
                    if (row.getFirstConnectedAt() == null) {
                        row.setFirstConnectedAt(LocalDateTime.now());
                    }
                    final CentralServer persisted = this.servers.repository().update(row);
                    this.registeredId = persisted.getId();
                    this.connected = true;
                    this.heartbeat = new HeartbeatScheduler(this.plugin, this.servers, this.logger, persisted);
                    this.heartbeat.start();
                    this.logger.info("CentralService online — serverUuid={}", this.context.serverUuid());
                })
                .exceptionally(ex -> {
                    this.logger.error("CentralService registration failed: {}", ex.getMessage());
                    return null;
                });
    }

    public void stop() {
        if (this.heartbeat != null) {
            this.heartbeat.stop();
            this.heartbeat = null;
        }
        this.connected = false;
        if (this.registeredId != null) {
            final Long id = this.registeredId;
            this.servers.repository().findByIdAsync(id).thenAccept(opt -> opt.ifPresent(fresh -> {
                fresh.setConnectionStatus(CentralServer.ConnectionStatus.DISCONNECTED);
                this.servers.repository().update(fresh);
            }));
            this.registeredId = null;
        }
    }

    public @NotNull ServerContext context() {
        return this.context;
    }

    public boolean isConnected() {
        return this.connected;
    }

    public int onlinePlayers() {
        return Bukkit.getOnlinePlayers().size();
    }
}
