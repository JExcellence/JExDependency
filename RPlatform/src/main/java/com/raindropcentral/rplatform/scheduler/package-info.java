/**
 * High level scheduling façade that bridges Folia's region model with the
 * traditional Bukkit scheduler used by Paper and Spigot.
 * <p>
 * {@link com.raindropcentral.rplatform.scheduler.ISchedulerAdapter} is created inside
 * {@link com.raindropcentral.rplatform.RPlatform#RPlatform(org.bukkit.plugin.java.JavaPlugin)}
 * using the detected {@link com.raindropcentral.rplatform.api.PlatformType}. When the platform is
 * {@link com.raindropcentral.rplatform.api.PlatformType#FOLIA}, the factory reflectively loads
 * {@code scheduler.impl.FoliaISchedulerImpl}. Paper and Spigot both map to the same
 * {@code scheduler.impl.BukkitISchedulerImpl}; if the Folia implementation cannot be created the
 * adapter automatically falls back to the Bukkit variant so the platform continues to boot.
 * </p>
 * <p>
 * {@link com.raindropcentral.rplatform.RPlatform#initialize()} offloads its bootstrap work via
 * {@link ISchedulerAdapter#runAsync(Runnable)}, keeping heavy translation and database setup away from
 * the primary server thread. Callers must treat asynchronous callbacks as running outside the tick
 * thread and synchronise shared state appropriately before touching Bukkit or Paper-only APIs. Once
 * the {@link java.util.concurrent.CompletableFuture} returned by {@code initialize()} completes, the
 * adapter can be used to register repeating jobs and {@link com.raindropcentral.rplatform.workload.WorkloadExecutor}
 * instances that consume their tick budget safely.
 * </p>
 * <p>
 * The expanded Javadoc on {@link ISchedulerAdapter} and its implementations documents the threading
 * guarantees and fallback behaviours available on Folia versus Bukkit/Paper, providing guidance on
 * when to select entity, region, or global execution.
 * </p>
 */
package com.raindropcentral.rplatform.scheduler;
