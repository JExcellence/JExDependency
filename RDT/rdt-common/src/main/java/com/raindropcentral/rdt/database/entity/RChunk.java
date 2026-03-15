package com.raindropcentral.rdt.database.entity;

import com.raindropcentral.rdt.utils.ChunkType;
import com.raindropcentral.rdt.utils.TownProtections;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Persistent representation of a claimed chunk belonging to a {@link RTown}.
 * <p>
 * Each chunk stores its X/Z coordinates and a {@link com.raindropcentral.rdt.utils.ChunkType} describing its role
 * inside the town (e.g., default). A chunk belongs to a single town.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.2
 */
@Entity
@Table(name = "r_chunks")
@SuppressWarnings({
        "unused",
        "DefaultAnnotationParam",
        "JpaDataSourceORMInspection"
})
public class RChunk extends BaseEntity {

    /** Owning {@link RTown}. Matches {@code mappedBy = "town"} in {@link RTown}. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "town_id", nullable = false)
    private RTown town;

    /** Chunk X coordinate. */
    @Column(name = "x_loc", unique = false, nullable = false)
    private int x_loc;

    /** Chunk Z coordinate. */
    @Column(name = "z_loc", unique = false, nullable = false)
    private int z_loc;

    /** Semantic chunk type for town systems. */
    @Column(name = "chunk_type", unique = false, nullable = false)
    private ChunkType type;

    /** World identifier for the currently placed chunk block, when present. */
    @Column(name = "chunk_block_world", unique = false, nullable = true)
    private String chunk_block_world;

    /** Block X coordinate for the currently placed chunk block, when present. */
    @Column(name = "chunk_block_x", unique = false, nullable = true)
    private Integer chunk_block_x;

    /** Block Y coordinate for the currently placed chunk block, when present. */
    @Column(name = "chunk_block_y", unique = false, nullable = true)
    private Integer chunk_block_y;

    /** Block Z coordinate for the currently placed chunk block, when present. */
    @Column(name = "chunk_block_z", unique = false, nullable = true)
    private Integer chunk_block_z;

    /** Role requirements overriding town-wide protection settings for this chunk. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "chunk_protection_overrides", joinColumns = @JoinColumn(name = "chunk_id"))
    @MapKeyColumn(name = "protection_key")
    @Column(name = "role_id", nullable = false)
    private final Map<String, String> protection_overrides = new HashMap<>();

    /** Required by Hibernate. */
    protected RChunk() {}

    /**
     * Construct a new town chunk.
     *
     * @param town   owning town
     * @param x_loc  chunk X coordinate
     * @param z_loc  chunk Z coordinate
     * @param type   type marker
     */
    public RChunk(RTown town, int x_loc, int z_loc, ChunkType type) {
        this.town = town;
        this.x_loc = x_loc;
        this.z_loc = z_loc;
        this.type = type;
    }

    /**
     * Returns the claimed chunk X coordinate.
     *
     * @return chunk X coordinate
     */
    public int getX_loc() { return this.x_loc; }

    /**
     * Returns the claimed chunk Z coordinate.
     *
     * @return chunk Z coordinate
     */
    public int getZ_loc() { return this.z_loc; }

    /**
     * Returns this claimed chunk's semantic type.
     *
     * @return chunk type
     */
    public ChunkType getType() { return this.type; }

    /**
     * Updates the semantic chunk type for this claim.
     *
     * @param type new chunk type
     */
    public void setType(final ChunkType type) {
        this.type = type;
    }

    /**
     * Returns the stored world name of the currently placed chunk block.
     *
     * @return chunk block world name or {@code null} when not placed
     */
    public @Nullable String getChunkBlockWorld() {
        return this.chunk_block_world;
    }

    /**
     * Returns the stored X coordinate of the currently placed chunk block.
     *
     * @return chunk block X or {@code null} when not placed
     */
    public @Nullable Integer getChunkBlockX() {
        return this.chunk_block_x;
    }

    /**
     * Returns the stored Y coordinate of the currently placed chunk block.
     *
     * @return chunk block Y or {@code null} when not placed
     */
    public @Nullable Integer getChunkBlockY() {
        return this.chunk_block_y;
    }

    /**
     * Returns the stored Z coordinate of the currently placed chunk block.
     *
     * @return chunk block Z or {@code null} when not placed
     */
    public @Nullable Integer getChunkBlockZ() {
        return this.chunk_block_z;
    }

    /**
     * Stores the placed chunk block location for this claim.
     *
     * @param worldName world name where the chunk block is placed
     * @param blockX block X coordinate
     * @param blockY block Y coordinate
     * @param blockZ block Z coordinate
     */
    public void setChunkBlockLocation(
            final @NotNull String worldName,
            final int blockX,
            final int blockY,
            final int blockZ
    ) {
        this.chunk_block_world = worldName;
        this.chunk_block_x = blockX;
        this.chunk_block_y = blockY;
        this.chunk_block_z = blockZ;
    }

    /**
     * Clears the stored chunk block location for this claim.
     */
    public void clearChunkBlockLocation() {
        this.chunk_block_world = null;
        this.chunk_block_x = null;
        this.chunk_block_y = null;
        this.chunk_block_z = null;
    }

    /**
     * Returns mutable protection overrides configured on this chunk.
     *
     * @return protection-key to role-ID overrides
     */
    public @NotNull Map<String, String> getProtectionOverrides() {
        return this.protection_overrides;
    }

    /**
     * Returns whether this chunk overrides a specific protection role.
     *
     * @param protection protection key to test
     * @return {@code true} when an explicit chunk override exists
     */
    public boolean hasProtectionOverride(final @NotNull TownProtections protection) {
        return this.protection_overrides.containsKey(protection.getProtectionKey());
    }

    /**
     * Returns the configured role ID override for a protection.
     *
     * @param protection protection key to resolve
     * @return normalized role ID override or {@code null} when no override exists
     */
    public @Nullable String getProtectionOverrideRoleId(final @NotNull TownProtections protection) {
        final String roleId = this.protection_overrides.get(protection.getProtectionKey());
        if (roleId == null || roleId.isBlank()) {
            return null;
        }
        return RTown.normalizeRoleId(roleId);
    }

    /**
     * Sets the role ID override for a protection on this chunk.
     *
     * @param protection protection to update
     * @param roleId role ID assigned to the protection
     */
    public void setProtectionOverrideRoleId(
            final @NotNull TownProtections protection,
            final @NotNull String roleId
    ) {
        this.protection_overrides.put(
                protection.getProtectionKey(),
                RTown.normalizeRoleId(roleId)
        );
    }

    /**
     * Removes the role override for a protection on this chunk.
     *
     * @param protection protection to clear
     */
    public void clearProtectionOverride(final @NotNull TownProtections protection) {
        this.protection_overrides.remove(protection.getProtectionKey());
    }
}
