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
 * Machine Fabrication System for automated crafting and resource processing.
 *
 * <p>This package provides a comprehensive framework for creating and managing
 * automated machines in RDQ. The system features:
 * <ul>
 *     <li>Multi-block structure construction and validation</li>
 *     <li>Permission-based machine unlocking and progression</li>
 *     <li>Automated crafting with fuel consumption</li>
 *     <li>Virtual storage management for inputs and outputs</li>
 *     <li>Upgradeable machine capabilities (speed, efficiency, output)</li>
 *     <li>Trust-based security for machine access control</li>
 * </ul>
 *
 * <h2>Core Components:</h2>
 * <ul>
 *     <li>{@link com.raindropcentral.rdq.machine.IMachineService} - Service interface for machine operations</li>
 *     <li>{@link com.raindropcentral.rdq.machine.type.EMachineType} - Machine type definitions</li>
 *     <li>{@link com.raindropcentral.rdq.machine.type.EMachineState} - Machine operational states</li>
 *     <li>{@link com.raindropcentral.rdq.machine.type.EUpgradeType} - Available upgrade types</li>
 *     <li>{@link com.raindropcentral.rdq.machine.type.EStorageType} - Storage classification types</li>
 * </ul>
 *
 * <h2>Architecture:</h2>
 * <p>The machine system follows a component-based architecture where each machine
 * type is composed of specialized components handling specific functionality:
 * <ul>
 *     <li>FabricatorComponent - Automated crafting logic</li>
 *     <li>StorageComponent - Virtual inventory management</li>
 *     <li>UpgradeComponent - Performance enhancement system</li>
 *     <li>FuelComponent - Energy consumption tracking</li>
 *     <li>RecipeComponent - Crafting recipe validation</li>
 *     <li>TrustComponent - Access control and security</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Create a new Fabricator machine
 * IMachineService machineService = rdq.getMachineService();
 * 
 * machineService.createMachine(player.getUniqueId(), EMachineType.FABRICATOR, location)
 *     .thenAccept(machine -> {
 *         // Configure the machine
 *         machineService.setRecipe(machine.getId(), recipeItems);
 *         machineService.addFuel(machine.getId(), 1000);
 *         machineService.toggleMachine(machine.getId(), true);
 *     });
 * }</pre>
 *
 * @author RaindropCentral
 * @version 1.0.0
 * @since 1.0.0
 */
package com.raindropcentral.rdq.machine;
