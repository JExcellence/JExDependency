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
 * GUI views for the machine fabrication system.
 *
 * <p>This package contains all inventory-based user interfaces for interacting with machines,
 * including:
 * <ul>
 *   <li>{@link com.raindropcentral.rdq.machine.view.MachineMainView} - Main machine control panel</li>
 *   <li>{@link com.raindropcentral.rdq.machine.view.MachineStorageView} - Storage management interface</li>
 *   <li>{@link com.raindropcentral.rdq.machine.view.MachineTrustView} - Trust list management</li>
 *   <li>{@link com.raindropcentral.rdq.machine.view.MachineUpgradeView} - Upgrade application interface</li>
 *   <li>{@link com.raindropcentral.rdq.machine.view.MachineRecipeView} - Recipe configuration interface</li>
 * </ul>
 *
 * <p>All views extend {@link com.raindropcentral.rplatform.view.BaseView} or
 * {@link com.raindropcentral.rplatform.view.APaginatedView} and integrate with the
 * JExTranslate i18n system for multi-language support.
 *
 * @author JExcellence
 * @version 1.0.0
 */
package com.raindropcentral.rdq.machine.view;
