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

package com.raindropcentral.rdq.database.converter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents the ConverterTool API type.
 */
public class ConverterTool {
	
	/**
	 * Sets a private field value using reflection.
	 *
	 * @param target the target object
	 * @param fieldName the name of the field to set
	 * @param value the value to set
	 */
	public void setPrivateField(
		@NotNull
		final Object target,
		@NotNull final String fieldName,
		@Nullable
		final Object value,
		Logger LOGGER
	) {
		try {
			final Field field = target.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(target, value);
		} catch (final NoSuchFieldException | IllegalAccessException e) {
			LOGGER.log(Level.SEVERE, "Failed to set field: " + fieldName, e);
			throw new RuntimeException("Failed to set field: " + fieldName, e);
		}
	}
}
