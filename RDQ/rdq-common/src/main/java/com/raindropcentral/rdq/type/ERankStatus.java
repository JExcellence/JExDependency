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
