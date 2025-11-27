package com.raindropcentral.rdq.external;

import com.raindropcentral.rplatform.logger.CentralLogger;
import com.raindropcentral.rplatform.utility.external.ExternalPluginService;
import net.luckperms.api.LuckPerms;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Typed init gate for LuckPerms.
 * Calls the provided callback exactly once after LuckPerms is bound.
 * Requires compileOnly/provided dependency on net.luckperms:api and no shading.
 */
public final class LuckPermsInitGate extends ExternalPluginService<LuckPerms> {

    private static final Logger LOGGER = CentralLogger.getLogger(LuckPermsInitGate.class.getName());
    private final Runnable onReady;
    private final AtomicBoolean executed = new AtomicBoolean(false);

    /**
     * @param required Whether this gate is required to succeed
     * @param onReady  Callback executed once when LuckPerms is bound (e.g., rankSystemFactory::initialize)
     */
    public LuckPermsInitGate(boolean required, Runnable onReady) {
        super(LuckPerms.class, "LuckPerms", required);
        this.onReady = Objects.requireNonNull(onReady, "onReady");
        // We don't want to call provider.initialize(); just our local callback.
        autoInitialize(false);
    }

    @Override
    protected void afterBind() {
        if (executed.compareAndSet(false, true)) {
            try {
                LOGGER.log(Level.INFO, "LuckPerms bound — executing initialization callback");
                onReady.run();
            } catch (Throwable t) {
                executed.set(false); // allow retry if needed
                LOGGER.log(Level.SEVERE, "Initialization callback failed after LuckPerms bind: " + t.getMessage(), t);
            }
        } else {
            LOGGER.log(Level.FINER, "Initialization callback already executed for LuckPerms");
        }
    }
}