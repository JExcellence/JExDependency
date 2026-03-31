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

package com.raindropcentral.rdq.view.ranks.grid;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for mapping between grid positions and inventory slot numbers.
 */
public class GridSlotMapper {
	
	private static final Map<GridPosition, Integer> RANK_SLOT_MAPPING = createRankSlotMapping();
	private static final List<Integer>              ALL_RANK_SLOT_NUMBERS = createAllRankSlotNumbers();
	
	/**
	 * Gets the inventory slot number for a given grid position.
	 */
	public static @Nullable Integer getSlotForPosition(
		final @NotNull GridPosition position
	) {
		
		return RANK_SLOT_MAPPING.get(position);
	}
	
	/**
	 * Gets the grid position for a given inventory slot number.
	 */
	public static @Nullable GridPosition getPositionForSlot(
		final int slotNumber
	) {
		
		for (final Map.Entry<GridPosition, Integer> entry : RANK_SLOT_MAPPING.entrySet()) {
			if (entry.getValue().equals(slotNumber)) {
				return entry.getKey();
			}
		}
		return null;
	}
	
	/**
	 * Gets all slot numbers that can contain rank content.
	 */
	public static @NotNull List<Integer> getAllRankSlotNumbers() {
		
		return new ArrayList<>(ALL_RANK_SLOT_NUMBERS);
	}
	
	/**
	 * Gets a copy of the complete slot mapping.
	 */
	public static @NotNull Map<GridPosition, Integer> getSlotMapping() {
		
		return new HashMap<>(RANK_SLOT_MAPPING);
	}
	
	/**
	 * Checks if a slot number is a valid rank content slot.
	 */
	public static boolean isValidRankSlot(
		final int slotNumber
	) {
		
		return ALL_RANK_SLOT_NUMBERS.contains(slotNumber);
	}
	
	private static @NotNull Map<GridPosition, Integer> createRankSlotMapping() {
		
		final Map<GridPosition, Integer> mapping = new HashMap<>();
		
		// Row 0 (top row, excluding navigation slots)
		mapping.put(
			new GridPosition(
				0,
				0
			),
			1
		);
		mapping.put(
			new GridPosition(
				1,
				0
			),
			2
		);
		mapping.put(
			new GridPosition(
				2,
				0
			),
			3
		);
		mapping.put(
			new GridPosition(
				3,
				0
			),
			5
		);
		mapping.put(
			new GridPosition(
				4,
				0
			),
			6
		);
		mapping.put(
			new GridPosition(
				5,
				0
			),
			7
		);
		
		// Row 1
		mapping.put(
			new GridPosition(
				0,
				1
			),
			10
		);
		mapping.put(
			new GridPosition(
				1,
				1
			),
			11
		);
		mapping.put(
			new GridPosition(
				2,
				1
			),
			12
		);
		mapping.put(
			new GridPosition(
				3,
				1
			),
			13
		);
		mapping.put(
			new GridPosition(
				4,
				1
			),
			14
		);
		mapping.put(
			new GridPosition(
				5,
				1
			),
			15
		);
		mapping.put(
			new GridPosition(
				6,
				1
			),
			16
		);
		
		// Row 2 (center row)
		mapping.put(
			new GridPosition(
				0,
				2
			),
			19
		);
		mapping.put(
			new GridPosition(
				1,
				2
			),
			20
		);
		mapping.put(
			new GridPosition(
				2,
				2
			),
			21
		);
		mapping.put(
			new GridPosition(
				3,
				2
			),
			22
		);
		mapping.put(
			new GridPosition(
				4,
				2
			),
			23
		);
		mapping.put(
			new GridPosition(
				5,
				2
			),
			24
		);
		mapping.put(
			new GridPosition(
				6,
				2
			),
			25
		);
		
		// Row 3
		mapping.put(
			new GridPosition(
				0,
				3
			),
			28
		);
		mapping.put(
			new GridPosition(
				1,
				3
			),
			29
		);
		mapping.put(
			new GridPosition(
				2,
				3
			),
			30
		);
		mapping.put(
			new GridPosition(
				3,
				3
			),
			31
		);
		mapping.put(
			new GridPosition(
				4,
				3
			),
			32
		);
		mapping.put(
			new GridPosition(
				5,
				3
			),
			33
		);
		mapping.put(
			new GridPosition(
				6,
				3
			),
			34
		);
		
		// Row 4 (bottom content row)
		mapping.put(
			new GridPosition(
				0,
				4
			),
			37
		);
		mapping.put(
			new GridPosition(
				1,
				4
			),
			38
		);
		mapping.put(
			new GridPosition(
				2,
				4
			),
			39
		);
		mapping.put(
			new GridPosition(
				3,
				4
			),
			40
		);
		mapping.put(
			new GridPosition(
				4,
				4
			),
			41
		);
		mapping.put(
			new GridPosition(
				5,
				4
			),
			42
		);
		mapping.put(
			new GridPosition(
				6,
				4
			),
			43
		);
		
		return mapping;
	}
	
	private static @NotNull List<Integer> createAllRankSlotNumbers() {
		
		final List<Integer> slots = new ArrayList<>();
		slots.addAll(List.of(
			1,
			2,
			3,
			5,
			6,
			7
		));
		slots.addAll(List.of(
			10,
			11,
			12,
			13,
			14,
			15,
			16
		));
		slots.addAll(List.of(
			19,
			20,
			21,
			22,
			23,
			24,
			25
		));
		slots.addAll(List.of(
			28,
			29,
			30,
			31,
			32,
			33,
			34
		));
		slots.addAll(List.of(
			37,
			38,
			39,
			40,
			41,
			42,
			43
		));
		return slots;
	}
	
}