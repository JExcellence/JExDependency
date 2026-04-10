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

package com.raindropcentral.rdt.service;

import com.raindropcentral.rdt.database.entity.RNation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Result of a nation-domain action that may create, update, or remove one nation.
 *
 * @param status action status
 * @param nation affected nation, or {@code null} when unavailable
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public record NationActionResult(
    @NotNull NationActionStatus status,
    @Nullable RNation nation
) {
}
