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

package com.raindropcentral.rda.database.entity;

/**
 * Enumerates the lifecycle states of a persisted RDA party invite.
 *
 * @author Codex
 * @since 1.3.0
 * @version 1.3.0
 */
public enum RDAPartyInviteStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    EXPIRED
}
