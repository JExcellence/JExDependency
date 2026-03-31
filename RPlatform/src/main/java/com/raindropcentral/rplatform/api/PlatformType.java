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

package com.raindropcentral.rplatform.api;

/**
 * Enumerates the server platform variants supported by the {@link PlatformAPI} abstraction.
 *
 * <p>The ordering reflects the detection priority used by {@link PlatformAPIFactory}.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public enum PlatformType {
    /** Folia servers featuring regionized execution and strict thread guarantees. */
    FOLIA,
    /** Paper servers (1.20+) exposing modern Adventure and scheduling APIs. */
    PAPER,
    /** Legacy Spigot or Bukkit servers lacking the newer platform integrations. */
    SPIGOT
}
