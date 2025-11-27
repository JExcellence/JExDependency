package com.raindropcentral.rdq.type;

import org.jetbrains.annotations.NotNull;

public enum EPerkIdentifier {
	
	SPEED("speed"),
	FIRE_RESISTANCE("fire_resistance"),
	SATURATION("saturation"),
	JUMP_BOOST("jump_boost"),
	NIGHT_VISION("night_vision"),
	STRENGTH("strength"),
	RESISTANCE("resistance"),
	GLOW("glow"),
	HASTE("haste")
	
	;
	
	private final String identifier;
	
	EPerkIdentifier(
		final @NotNull String identifier
	) {
		this.identifier = identifier;
	}
	
	public String getIdentifier() {
		
		return this.identifier;
	}
}
