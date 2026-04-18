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
 * Contains JPA entity classes for the machine fabrication system.
 *
 * <p>This package includes entities for:
 * <ul>
 *   <li>{@link com.raindropcentral.rdq.database.entity.machine.Machine} - Core machine entity</li>
 *   <li>{@link com.raindropcentral.rdq.database.entity.machine.MachineStorage} - Machine storage slots</li>
 *   <li>{@link com.raindropcentral.rdq.database.entity.machine.MachineTrust} - Machine access control</li>
 *   <li>{@link com.raindropcentral.rdq.database.entity.machine.MachineUpgrade} - Machine upgrades</li>
 * </ul>
 *
 * <p>All entities in this package are automatically discovered by Hibernate through
 * the {@code database.entity} package scanning mechanism.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 1.0.0
 */
package com.raindropcentral.rdq.database.entity.machine;
