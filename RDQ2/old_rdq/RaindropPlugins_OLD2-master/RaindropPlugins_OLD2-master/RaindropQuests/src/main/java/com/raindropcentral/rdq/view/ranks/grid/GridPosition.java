package com.raindropcentral.rdq.view.ranks.grid;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Represents a position in the rank grid coordinate system.
 */
public class GridPosition {
	
	public final int x;
	public final int y;
	
	public GridPosition(
		final int x,
		final int y
	) {
		
		this.x = x;
		this.y = y;
	}
	
	/**
	 * Creates a new GridPosition offset by the given deltas.
	 */
	public @NotNull GridPosition offset(
		final int deltaX,
		final int deltaY
	) {
		
		return new GridPosition(
			this.x + deltaX,
			this.y + deltaY
		);
	}
	
	/**
	 * Calculates the distance to another grid position.
	 */
	public double distanceTo(
		final @NotNull GridPosition other
	) {
		
		final int deltaX = this.x - other.x;
		final int deltaY = this.y - other.y;
		return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
	}
	
	@Override
	public boolean equals(
		final Object obj
	) {
		
		if (this == obj)
			return true;
		if (obj == null || this.getClass() != obj.getClass())
			return false;
		final GridPosition that = (GridPosition) obj;
		return this.x == that.x && this.y == that.y;
	}
	
	@Override
	public int hashCode() {
		
		return Objects.hash(
			this.x,
			this.y
		);
	}
	
	@Override
	public String toString() {
		
		return "GridPosition{x=" + this.x + ", y=" + this.y + '}';
	}
	
}