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
 * Provides comprehensive collection and delivery of vanilla Minecraft statistics.
 *
 * <p>This package implements a complete system for capturing all 300+ native Minecraft
 * statistics tracked by the client, including detailed breakdowns by material type,
 * entity type, and action category. The system integrates with the existing Statistics
 * Delivery System to provide complete telemetry for player behavior analysis.
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link com.raindropcentral.core.service.statistics.vanilla.StatisticCategory} - Categorization of statistics</li>
 *   <li>{@link com.raindropcentral.core.service.statistics.vanilla.config.VanillaStatisticConfig} - Configuration management</li>
 *   <li>Collection services - Periodic and event-driven statistic collection</li>
 *   <li>Cache management - Delta computation and persistence</li>
 *   <li>Aggregation - Computed summary statistics</li>
 * </ul>
 *
 * @author JExcellence
 * @since 1.0.0
 */
package com.raindropcentral.core.service.statistics.vanilla;
