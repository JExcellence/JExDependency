package com.raindropcentral.rdq.view.rank.grid;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class GridPosition {

    private final int x;
    private final int y;

    public GridPosition(final int x, final int y) {
        this.x = x;
        this.y = y;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public @NotNull GridPosition offset(final int deltaX, final int deltaY) {
        return new GridPosition(x + deltaX, y + deltaY);
    }

    public double distanceTo(final @NotNull GridPosition other) {
        final int deltaX = x - other.x;
        final int deltaY = y - other.y;
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final GridPosition that = (GridPosition) o;
        return x == that.x && y == that.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "GridPosition{x=" + x + ", y=" + y + '}';
    }
}