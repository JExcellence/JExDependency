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

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Defines the serialized contract implemented by RDS shop item payloads.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
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
