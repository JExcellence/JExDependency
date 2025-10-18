/**
 * Scheduler implementations that are discovered reflectively by
 * {@link com.raindropcentral.rplatform.scheduler.ISchedulerAdapter#create(org.bukkit.plugin.java.JavaPlugin,
 * com.raindropcentral.rplatform.api.PlatformType)}.
 * <p>
 * {@link com.raindropcentral.rplatform.scheduler.impl.FoliaISchedulerImpl} binds to Folia's
 * region, entity, and global schedulers via reflection so the class loads only when those APIs are
 * available. {@link com.raindropcentral.rplatform.scheduler.impl.BukkitISchedulerImpl} delegates to the
 * legacy Bukkit scheduler that Paper and Spigot expose. Both implementations wrap every task in
 * defensive logging so asynchronous failures surface quickly. When running workloads or bootstrap
 * routines, prefer scheduling through the adapter instead of calling Bukkit APIs directly so the
 * correct implementation is chosen for the active server flavour.
 * </p>
 * <p>
 * Folia's adapter may complete work on worker threads when {@link com.raindropcentral.rplatform.scheduler.ISchedulerAdapter#runAsync(Runnable)}
 * is used; callers that mutate Bukkit state must hop back to {@link com.raindropcentral.rplatform.scheduler.ISchedulerAdapter#runSync(Runnable)}
 * or one of the region-aware helpers first. The Bukkit implementation always executes synchronous
 * work on the primary server thread but still dispatches asynchronous tasks off-thread, so shared
 * data structures must be synchronised just like they are with Folia.
 * </p>
 */
package com.raindropcentral.rplatform.scheduler.impl;
