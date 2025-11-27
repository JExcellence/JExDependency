package com.raindropcentral.rdq.perk.runtime;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class DefaultPerkTriggerService implements PerkTriggerService, Listener {

    private static final Logger LOGGER = CentralLogger.getLogger(DefaultPerkTriggerService.class.getName());
    private static final long EXCEPTION_THROTTLE_MILLIS = 5_000L;

    private final RDQ rdq;
    private final DefaultPerkRegistry perkRegistry;
    private final PerkAuditService auditService;
    private final AtomicLong triggerFailureWindow = new AtomicLong();

    public DefaultPerkTriggerService(@NotNull RDQ rdq, @NotNull DefaultPerkRegistry perkRegistry, @NotNull PerkAuditService auditService) {
        this.rdq = rdq;
        this.perkRegistry = perkRegistry;
        this.auditService = auditService;
    }

    @Override
    public void handleEvent(@NotNull Event event, @NotNull Player player) {
        var eventName = event.getClass().getSimpleName();
        for (var runtime : perkRegistry.getAllPerkRuntimes()) {
            if (!runtime.getType().isEventBased()) continue;
            
            if (!runtime.supports(event)) {
                var context = new LinkedHashMap<String, Object>();
                context.put("reason", "unsupported");
                auditService.recordTrigger(runtime.getId(), player.getUniqueId(), eventName, false, "unsupported-event", context, null);
                LOGGER.log(Level.FINEST, "Skipping perk {0} for event {1} due to unsupported trigger", new Object[]{runtime.getId(), eventName});
                continue;
            }
            
            var playerId = player.getUniqueId();
            var fingerprint = auditService.fingerprint(playerId);
            if (!runtime.canActivate(player) && !runtime.isActive(player)) {
                var context = new LinkedHashMap<String, Object>();
                context.put("reason", "eligibility");
                auditService.recordTrigger(runtime.getId(), playerId, eventName, false, "not-eligible", context, null);
                LOGGER.log(Level.FINEST, "Perk {0} not eligible for player fingerprint {1} on event {2}", new Object[]{runtime.getId(), fingerprint, eventName});
                continue;
            }
            
            try {
                runtime.trigger(player, eventName);
                LOGGER.log(Level.FINE, "Triggered perk {0} for player fingerprint {1} via event {2}", new Object[]{runtime.getId(), fingerprint, eventName});
            } catch (Exception exception) {
                auditService.recordTrigger(runtime.getId(), playerId, eventName, false, "exception", null, exception);
                logThrottledFailure(runtime.getId(), fingerprint, eventName, exception);
            }
        }
    }

    @Override
    public void registerListeners() {
        Bukkit.getServer().getPluginManager().registerEvents(this, rdq.getPlugin());
        LOGGER.log(Level.FINE, "Registered perk trigger listeners");
    }

    @Override
    public void unregisterListeners() {
        LOGGER.log(Level.FINE, "Perk trigger listeners will be unregistered automatically on plugin shutdown");
    }

    private void logThrottledFailure(String perkId, String fingerprint, String eventName, Throwable cause) {
        var now = System.currentTimeMillis();
        long previous;
        do {
            previous = triggerFailureWindow.get();
            if (previous != 0L && (now - previous) < EXCEPTION_THROTTLE_MILLIS) return;
        } while (!triggerFailureWindow.compareAndSet(previous, now));
        
        var record = new LogRecord(Level.WARNING, "Failed to trigger perk {0} for player fingerprint {1} via event {2}");
        record.setLoggerName(LOGGER.getName());
        record.setParameters(new Object[]{perkId, fingerprint, eventName});
        record.setThrown(cause);
        LOGGER.log(record);
    }
}