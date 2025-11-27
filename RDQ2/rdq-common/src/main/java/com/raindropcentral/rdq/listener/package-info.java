/**
 * Bukkit listeners automatically registered alongside RDQ commands.
 * <p>
 * These listeners are discovered by the shared
 * {@link com.raindropcentral.commands.CommandFactory} during component
 * initialization, immediately after managers such as the bounty subsystem are
 * constructed. As a result the listeners can safely call into
 * {@link com.raindropcentral.rdq.RDQ#getBountyManager()} and other services as
 * soon as the plugin is enabled.
 * </p>
 * <p>
 * Listener callbacks execute on the Bukkit primary thread. When long-running or
 * asynchronous work is required, delegate to {@link com.raindropcentral.rdq.RDQ#getExecutor()}
 * and return to the main thread through the plugin's {@code runSync} helper
 * (invoked from {@link com.raindropcentral.rdq.RDQ#onEnable()} during lifecycle
 * orchestration). This pattern keeps Bukkit state mutations within the correct
 * thread boundary while still allowing background computation.
 * </p>
 */
package com.raindropcentral.rdq.listener;
