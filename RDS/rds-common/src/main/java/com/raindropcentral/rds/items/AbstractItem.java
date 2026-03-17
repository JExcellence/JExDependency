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

package com.raindropcentral.rds.items;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Represents abstract item.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@JsonTypeInfo(
	use = JsonTypeInfo.Id.NAME,
	property = "type"
)
@JsonSubTypes({
	@JsonSubTypes.Type(value = ShopItem.class, name = "ITEM"),
})
/**
 * Represents the AbstractItem API type.
 */
@JsonIgnoreProperties(value = {"typeId", "estimatedValue", "descriptionKey"}, allowGetters = true)
public abstract class AbstractItem implements Item {
	
	/**
	 * Returns the type id.
	 *
	 * @return the type id
	 */
	@Override
	public abstract @NotNull String getTypeId();
	
	/**
	 * Grants this item payload to the supplied player.
	 *
	 * @param player target player
	 * @return the grant result
	 */
	@Override
	public abstract @NotNull CompletableFuture<Boolean> grant(@NotNull Player player);
	
	/**
	 * Returns the estimated value.
	 *
	 * @return the estimated value
	 */
	@Override
	public abstract double getEstimatedValue();
	
	/**
	 * Returns the description key.
	 *
	 * @return the description key
	 */
	@Override
	public @NotNull String getDescriptionKey() {
		return "reward." + getTypeId().toLowerCase() + ".description";
	}
	
	/**
	 * Validates the current abstract item.
	 *
	 * @throws IllegalArgumentException if the current value set is invalid
	 */
	public void validate() {
	}
	
}
