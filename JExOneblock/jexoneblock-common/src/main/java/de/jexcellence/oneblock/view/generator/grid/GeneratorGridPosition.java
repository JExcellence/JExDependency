package de.jexcellence.oneblock.view.generator.grid;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Represents a position in the generator structure grid coordinate system.
 * Similar to RDQ's GridPosition but adapted for generator structures.
 */
public class GeneratorGridPosition {
    
    public final int x;
    public final int z;
    
    public GeneratorGridPosition(
        final int x,
        final int z
    ) {
        this.x = x;
        this.z = z;
    }
    
    /**
     * Creates a new GeneratorGridPosition offset by the given deltas.
     */
    public @NotNull GeneratorGridPosition offset(
        final int deltaX,
        final int deltaZ
    ) {
        return new GeneratorGridPosition(
            this.x + deltaX,
            this.z + deltaZ
        );
    }
    
    /**
     * Calculates the distance to another grid position.
     */
    public double distanceTo(
        final @NotNull GeneratorGridPosition other
    ) {
        final int deltaX = this.x - other.x;
        final int deltaZ = this.z - other.z;
        return Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
    }
    
    /**
     * Checks if this position is within the given bounds.
     */
    public boolean isWithinBounds(
        final int minX,
        final int minZ,
        final int maxX,
        final int maxZ
    ) {
        return this.x >= minX && this.x <= maxX && this.z >= minZ && this.z <= maxZ;
    }
    
    @Override
    public boolean equals(
        final Object obj
    ) {
        if (this == obj)
            return true;
        if (obj == null || this.getClass() != obj.getClass())
            return false;
        final GeneratorGridPosition that = (GeneratorGridPosition) obj;
        return this.x == that.x && this.z == that.z;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(
            this.x,
            this.z
        );
    }
    
    @Override
    public String toString() {
        return "GeneratorGridPosition{x=" + this.x + ", z=" + this.z + '}';
    }
}