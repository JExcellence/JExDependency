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

package com.raindropcentral.rdq.type;

/**
 * Represents the ERankStatus API type.
 */
public enum ERankStatus {
	/**
	 * The player owns this rank.
	 */
	OWNED,
	
	/**
	 * The rank is available for the player to start working on.
	 */
	AVAILABLE,
	
	/**
	 * The player is currently working on this rank's requirements.
	 */
	IN_PROGRESS,
	
	/**
	 * The rank is locked and cannot be accessed yet.
	 */
	LOCKED
}
