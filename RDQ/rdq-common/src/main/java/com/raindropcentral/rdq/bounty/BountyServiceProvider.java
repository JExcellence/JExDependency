package com.raindropcentral.rdq.bounty;

import org.jetbrains.annotations.NotNull;

import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service provider for IBountyService implementations.
 * Automatically loads the correct implementation (Free or Premium) based on the classpath.
 * 
 * This uses Java's ServiceLoader mechanism to discover implementations at runtime.
 * The rdq-free module provides FreeBountyService, and rdq-premium provides PremiumBountyService.
 * 
 * @author JExcellence
 * @version 1.0.0
 * @since 6.0.0
 */
public class BountyServiceProvider {

    private static final Logger LOGGER = Logger.getLogger(BountyServiceProvider.class.getName());
    private static IBountyService instance;

    /**
     * Gets the singleton instance of the bounty service.
     * The implementation is automatically selected based on which module is loaded.
     * 
     * @return the bounty service instance (Free or Premium)
     * @throws IllegalStateException if no implementation is found
     */
    @NotNull
    public static IBountyService getInstance() {
        if (instance == null) {
            synchronized (BountyServiceProvider.class) {
                if (instance == null) {
                    instance = loadService();
                }
            }
        }
        return instance;
    }

    /**
     * Loads the bounty service implementation using ServiceLoader.
     * 
     * @return the loaded service implementation
     * @throws IllegalStateException if no implementation is found
     */
    private static IBountyService loadService() {
        // Use the current thread's context classloader to handle plugin classloader isolation
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = BountyServiceProvider.class.getClassLoader();
        }
        
        ServiceLoader<IBountyService> loader = ServiceLoader.load(IBountyService.class, classLoader);
        
        for (IBountyService service : loader) {
            String version = service.isPremium() ? "Premium" : "Free";
            LOGGER.log(Level.INFO, "Loaded " + version + " Bounty Service: " + service.getClass().getName());
            return service;
        }
        
        throw new IllegalStateException(
            "No IBountyService implementation found! " +
            "Make sure either rdq-free or rdq-premium is on the classpath."
        );
    }

    /**
     * Manually sets the bounty service instance.
     * Useful for testing or custom implementations.
     * 
     * @param service the service to use
     */
    public static void setInstance(@NotNull IBountyService service) {
        synchronized (BountyServiceProvider.class) {
            instance = service;
            String version = service.isPremium() ? "Premium" : "Free";
            LOGGER.log(Level.INFO, "Manually set " + version + " Bounty Service");
        }
    }

    /**
     * Resets the service instance, forcing a reload on next access.
     * Useful for testing.
     */
    public static void reset() {
        synchronized (BountyServiceProvider.class) {
            instance = null;
        }
    }

    /**
     * Checks if a bounty service is currently loaded.
     * 
     * @return true if a service is loaded, false otherwise
     */
    public static boolean isLoaded() {
        return instance != null;
    }

    /**
     * Gets the current service type.
     * 
     * @return "Premium", "Free", or "None" if no service is loaded
     */
    @NotNull
    public static String getServiceType() {
        if (instance == null) {
            return "None";
        }
        return instance.isPremium() ? "Premium" : "Free";
    }
}
