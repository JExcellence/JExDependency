package com.raindropcentral.rdq.external;

import com.raindropcentral.rplatform.logger.CentralLogger;
import com.raindropcentral.rplatform.utility.external.ExternalPluginService;
import com.raindropcentral.rcore.api.RCoreAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Typed binder for RCoreAdapter.
 * - Extends ExternalPluginService<RCoreAdapter> so we get a typed instance when bound.
 * - Invokes a caller-provided Consumer with the typed instance.
 *
 * Note: Requires RDQ to depend on the RCore API (compileOnly/provided) and not shade it.
 */
public final class RCoreAdapterService extends ExternalPluginService<RCoreAdapter> {

    private static final Logger LOGGER = CentralLogger.getLogger(RCoreAdapterService.class.getName());
    private final Consumer<RCoreAdapter> binder;

    /**
     * @param providerPluginName The plugin name providing the adapter (e.g. "RCore")
     * @param required           Whether this service is required
     * @param binder             Callback invoked with a typed RCoreAdapter when bound
     */
    public RCoreAdapterService(@NotNull String providerPluginName,
                               boolean required,
                               @NotNull Consumer<RCoreAdapter> binder) {
        super(RCoreAdapter.class, providerPluginName, required);
        this.binder = binder;
        // We don't want to call initialize() on the provider; we only bind the typed instance
        autoInitialize(false);
    }

    @Override
    protected void afterBind() {
        Optional<RCoreAdapter> typed = getTyped();
        if (typed.isPresent()) {
            try {
                binder.accept(typed.get());
                LOGGER.log(Level.INFO, "RCoreAdapter bound (typed)");
            } catch (Throwable t) {
                LOGGER.log(Level.SEVERE, "Failed to bind RCoreAdapter into consumer: " + t.getMessage(), t);
            }
        } else {
            LOGGER.log(Level.SEVERE, "RCoreAdapter bound but not castable (classloader mismatch). Ensure RCore API is not shaded and is on the classpath.");
        }
    }
}