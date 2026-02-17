package com.raindropcentral.rdq.economy;

import org.jetbrains.annotations.Nullable;

/**
 * Static holder for the economy service instance.
 * Allows requirements and rewards to access the economy system.
 */
public final class EconomyServiceHolder {
	
	private static EconomyService instance;
	
	private EconomyServiceHolder() {
		// Utility class
	}
	
	public static void setInstance(@Nullable EconomyService service) {
		instance = service;
	}
	
	@Nullable
	public static EconomyService getInstance() {
		return instance;
	}
	
	public static boolean isAvailable() {
		return instance != null && instance.isAvailable();
	}
}
