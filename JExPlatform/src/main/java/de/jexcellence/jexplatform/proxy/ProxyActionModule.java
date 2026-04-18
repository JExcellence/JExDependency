package de.jexcellence.jexplatform.proxy;

import org.jetbrains.annotations.NotNull;

/**
 * SPI for module registration of proxy action handlers.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public interface ProxyActionModule {

    /**
     * Returns the stable module identifier used for routing proxy actions.
     *
     * @return the module identifier
     */
    @NotNull String moduleId();

    /**
     * Registers this module's proxy actions into the supplied proxy service.
     *
     * @param proxyService the active proxy service
     */
    void registerHandlers(@NotNull ProxyService proxyService);
}
