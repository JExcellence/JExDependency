package com.raindropcentral.rdq2.view.rank.grid;

import org.jetbrains.annotations.NotNull;

public record GridPosition(int x, int y) {

    public @NotNull GridPosition offset(final int deltaX, final int deltaY) {
        return new GridPosition(x + deltaX, y + deltaY);
    }

    public double distanceTo(final @NotNull GridPosition other) {
        final int deltaX = x - other.x;
        final int deltaY = y - other.y;
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }
}