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
 * Provides privacy controls for vanilla statistic telemetry.
 *
 * <p>{@link com.raindropcentral.core.service.statistics.vanilla.privacy.PlayerPrivacyManager} tracks per-player collection opt-out state,
 * while {@link com.raindropcentral.core.service.statistics.vanilla.privacy.UuidAnonymizer} produces deterministic anonymized identifiers for
 * analytics and other workflows that should not expose raw player UUIDs.
 */
package com.raindropcentral.core.service.statistics.vanilla.privacy;
