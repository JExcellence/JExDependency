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
 * Machine component implementations for the fabrication system.
 *
 * <p>This package contains component classes that encapsulate specific aspects
 * of machine functionality. Each component is responsible for a distinct concern:
 * <ul>
 *     <li>{@link com.raindropcentral.rdq.machine.component.FabricatorComponent} - Crafting logic and recipe management</li>
 *     <li>{@link com.raindropcentral.rdq.machine.component.StorageComponent} - Virtual storage management</li>
 *     <li>{@link com.raindropcentral.rdq.machine.component.UpgradeComponent} - Upgrade validation and effects</li>
 *     <li>{@link com.raindropcentral.rdq.machine.component.FuelComponent} - Fuel tracking and consumption</li>
 *     <li>{@link com.raindropcentral.rdq.machine.component.RecipeComponent} - Recipe validation and storage</li>
 *     <li>{@link com.raindropcentral.rdq.machine.component.TrustComponent} - Trust list and permission management</li>
 * </ul>
 *
 * <p>Components follow a composition pattern where each component operates on a
 * {@link com.raindropcentral.rdq.database.entity.machine.Machine} entity and provides
 * specific functionality without tight coupling to other components.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 1.0.0
 */
package com.raindropcentral.rdq.machine.component;
