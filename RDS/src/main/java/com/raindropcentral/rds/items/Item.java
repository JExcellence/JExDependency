/*
 * Item.java
 *
 * @author ItsRainingHP
 * @version 5.0.0
 */

package com.raindropcentral.rds.items;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Defines the serialized contract implemented by RDS shop item payloads.
 */
public interface Item {

	/**
	 * Returns the serialized type identifier for this item payload.
	 *
	 * @return the serialized type identifier
	 */
	@NotNull String getTypeId();

	/**
	 * Grants this item payload to the supplied player.
	 *
	 * @param player target player
	 * @return a future that completes with the grant result
	 */
	@NotNull
	CompletableFuture<Boolean> grant(@NotNull Player player);

	/**
	 * Returns the estimated value.
	 *
	 * @return the estimated value
	 */
	double getEstimatedValue();

	/**
	 * Returns the translation key describing this item payload.
	 *
	 * @return the translation key for this item payload
	 */
	@JsonIgnore
	@NotNull String getDescriptionKey();

}
