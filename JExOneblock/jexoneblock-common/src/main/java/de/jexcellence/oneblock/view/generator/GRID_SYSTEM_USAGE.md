# Generator Grid System Usage Guide

This document explains how to use the new grid-based visualization system for generator structures.

## Overview

The grid system provides a navigable view of generator structures that can handle any size structure by implementing viewport-based navigation, similar to the RDQ rank system.

## Key Components

### 1. GeneratorGridPosition
- Represents positions in the structure grid (x, z coordinates)
- Provides utility methods for distance calculation and bounds checking

### 2. GeneratorGridSlotMapper
- Maps between chest inventory slots and grid positions
- Manages navigation controls and reserved slots
- Provides constants for navigation slot positions

### 3. Updated Views

#### GeneratorVisualization3DView
- **Purpose**: Layer-by-layer structure visualization with navigation
- **Features**: 
  - Navigate around large structures using arrow controls
  - Switch between layers using up/down controls
  - Click on blocks for detailed information
  - Center view button to reset viewport

#### GeneratorBuildProgressView
- **Purpose**: Build progress visualization with structure grid
- **Features**:
  - Shows build state of each block (completed, in-progress, missing, incorrect)
  - Navigate around the structure during building
  - Real-time progress updates
  - Cancel build functionality

## Navigation Controls

```
Chest Layout (54 slots):
┌─────────────────────────────────────────────────────────────────────────────┐
│ [I] [ ] [ ] [↑] [ ] [ ] [ ] [L↑] [ ]  ← Info, Navigation Up, Layer Up       │
│ [←] [S] [S] [S] [S] [S] [S] [S] [→]  ← Nav Left, Structure Grid, Nav Right  │
│ [ ] [S] [S] [S] [S] [S] [S] [S] [ ]  ← Structure Grid                       │
│ [ ] [S] [S] [S] [S] [S] [S] [S] [ ]  ← Structure Grid                       │
│ [ ] [S] [S] [S] [S] [S] [S] [S] [ ]  ← Structure Grid                       │
│ [B] [ ] [ ] [↓] [ ] [ ] [ ] [L↓] [⌂]  ← Back, Navigation Down, Layer Down, Center │
└─────────────────────────────────────────────────────────────────────────────┘

Legend:
I = Info button
↑/↓/←/→ = Navigation arrows
L↑/L↓ = Layer up/down
S = Structure display slots
B = Back button
⌂ = Center view button
```

## Slot Mapping

The grid system uses specific slots for different purposes:

### Navigation Slots
- `NAVIGATION_UP_SLOT = 4` - Move viewport north
- `NAVIGATION_DOWN_SLOT = 49` - Move viewport south  
- `NAVIGATION_LEFT_SLOT = 18` - Move viewport west
- `NAVIGATION_RIGHT_SLOT = 26` - Move viewport east
- `CENTER_VIEW_SLOT = 53` - Reset viewport to center

### Layer Controls
- `LAYER_UP_SLOT = 8` - Go to higher layer
- `LAYER_DOWN_SLOT = 44` - Go to lower layer

### Utility Slots
- `INFO_SLOT = 0` - Information panel
- `BACK_BUTTON_SLOT = 45` - Back/Cancel button

### Structure Display Slots
The remaining slots (approximately 35 slots) are used for displaying the structure blocks.

## Usage Examples

### Opening the Grid Visualization
```java
// From GeneratorDesignDetailView
click.openForPlayer(GeneratorVisualization3DView.class, Map.of(
    "plugin", plugin.get(click),
    "structureManager", manager,
    "design", design,
    "playerStructure", null // For preview mode
));
```

### Opening Build Progress View
```java
// From build service
context.openForPlayer(GeneratorBuildProgressView.class, Map.of(
    "plugin", pluginInstance,
    "structureManager", manager,
    "design", design,
    "playerStructure", playerStructure
));
```

## Benefits

1. **Scalability**: Can display structures of any size
2. **Familiar UX**: Uses the same navigation patterns as RDQ rank system
3. **Performance**: Only renders visible portions of the structure
4. **Interactive**: Click on blocks for detailed information
5. **Real-time Updates**: Build progress updates in real-time

## Future Enhancements

1. **Minimap**: Show overall structure layout with viewport indicator
2. **Build Tools**: Integrated building assistance
3. **Material Requirements**: Show required materials for current viewport
4. **3D Isometric View**: Better depth perception
5. **Hologram Integration**: In-world structure preview

## Migration from Old System

The old 5x5 fixed preview grid has been replaced with this dynamic system. Views that previously used fixed grids now support:

- Structures larger than 5x5
- Layer-by-layer navigation
- Viewport-based navigation for large structures
- Interactive block clicking
- Real-time state updates

This provides a much better user experience for complex generator structures while maintaining the familiar chest-based UI.