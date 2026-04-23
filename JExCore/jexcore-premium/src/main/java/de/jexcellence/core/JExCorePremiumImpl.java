package de.jexcellence.core;

import com.raindropcentral.commands.CommandFactory;
import com.raindropcentral.commands.v2.argument.ArgumentTypeRegistry;
import de.jexcellence.core.command.R18nCommandMessages;
import de.jexcellence.core.stats.CentralTransport;
import de.jexcellence.core.stats.HttpCentralTransport;
import de.jexcellence.core.stats.StatisticChangeBridge;
import de.jexcellence.core.stats.StatisticsConfig;
import de.jexcellence.core.stats.StatisticsDelivery;
import de.jexcellence.core.stats.command.StatisticsHandler;
import de.jexcellence.dependency.delegate.AbstractPluginDelegate;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Premium edition delegate. Instantiates a {@link JExCore} orchestrator,
 * forwards lifecycle events, and installs {@code jexcore-stats}.
 */
public final class JExCorePremiumImpl extends AbstractPluginDelegate<JExCorePremium> {

    private static final Logger LOGGER = Logger.getLogger(JExCorePremiumImpl.class.getName());

    private JExCore core;
    private StatisticsDelivery delivery;

    public JExCorePremiumImpl(@NotNull JExCorePremium plugin) {
        super(plugin);
    }

    @Override
    public void onLoad() {
        try {
            this.core = new JExCore(getPlugin(), "Premium") {
                @Override
                protected int metricsId() {
                    return 0;
                }

                @Override
                protected void onReady() {
                    installStatistics(this);
                }

                @Override
                protected void onShutdown() {
                    if (JExCorePremiumImpl.this.delivery != null) {
                        JExCorePremiumImpl.this.delivery.shutdown(getPlugin());
                        JExCorePremiumImpl.this.delivery = null;
                    }
                }
            };
            this.core.onLoad();
        } catch (final Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed to load JExCore Premium", ex);
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void onEnable() {
        if (this.core == null) {
            LOGGER.severe("Cannot enable - JExCore Premium failed during onLoad.");
            getPlugin().getServer().getPluginManager().disablePlugin(getPlugin());
            return;
        }
        this.core.onEnable();
    }

    @Override
    public void onDisable() {
        try {
            if (this.core != null) {
                this.core.onDisable();
            }
        } catch (final Exception ex) {
            LOGGER.log(Level.SEVERE, "Error during JExCore Premium shutdown", ex);
        }
    }

    private void installStatistics(@NotNull JExCore core) {
        final String endpointStr = getPlugin().getConfig().getString("stats.endpoint", "");
        if (endpointStr.isBlank()) {
            core.logger().info("Statistics delivery disabled — set stats.endpoint in config.yml to enable.");
            return;
        }
        try {
            final StatisticsConfig config = StatisticsConfig.defaults(URI.create(endpointStr));
            final UUID serverUuid = UUID.nameUUIDFromBytes(("jexcore:" + Bukkit.getServer().getName()).getBytes());
            final CentralTransport transport = new HttpCentralTransport(config);
            this.delivery = StatisticsDelivery.install(getPlugin(), config, serverUuid, transport, core.logger());

            getPlugin().getServer().getPluginManager().registerEvents(
                    new StatisticChangeBridge(this.delivery), getPlugin());

            final var factory = new CommandFactory(getPlugin(), core);
            factory.registerTree("commands/jexstats.yml",
                    new StatisticsHandler(this.delivery).handlerMap(),
                    new R18nCommandMessages(),
                    ArgumentTypeRegistry.defaults());
        } catch (final Exception ex) {
            core.logger().error("Failed to install statistics delivery: {}", ex.getMessage());
        }
    }
}
