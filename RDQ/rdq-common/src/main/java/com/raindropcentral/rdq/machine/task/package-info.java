/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

/**
 * Provides asynchronous task implementations for machine operations.
 *
 * <p>This package contains {@link org.bukkit.scheduler.BukkitRunnable} implementations
 * that handle periodic machine operations such as crafting cycles and auto-save functionality.
 * Tasks in this package are designed to run asynchronously where possible and coordinate
 * with the main thread for Bukkit API operations.
 *
 * <p>Key classes:
 * <ul>
 *   <li>{@link com.raindropcentral.rdq.machine.task.MachineCraftingTask} - Handles automated crafting cycles</li>
 *   <li>{@link com.raindropcentral.rdq.machine.task.MachineAutoSaveTask} - Handles periodic machine data persistence</li>
 * </ul>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 1.0.0
 */
package com.raindropcentral.rdq.machine.task;
