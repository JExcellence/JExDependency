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
 * Contains quest task handlers and their registration infrastructure.
 *
 * <p>This package maps in-game activities such as combat, crafting, movement,
 * and collection events to quest task progress updates, and provides the manager
 * responsible for wiring enabled handlers into the RDQ runtime.</p>
 */
package com.raindropcentral.rdq.quest.handler;
