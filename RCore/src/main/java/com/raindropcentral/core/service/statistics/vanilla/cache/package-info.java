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
 * Provides caching for vanilla statistic snapshots.
 *
 * <p>{@link com.raindropcentral.core.service.statistics.vanilla.cache.StatisticCacheManager} stores per-player statistic baselines in
 * memory and on disk so callers can compute deltas, survive restarts, and
 * avoid re-sending unchanged values to downstream delivery systems.
 */
package com.raindropcentral.core.service.statistics.vanilla.cache;
